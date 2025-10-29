package com.example.jobsboard.algebra

import cats.*
import cats.implicits.*
import org.typelevel.log4cats.Logger
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams
import com.stripe.{Stripe => Stripe_}

import scala.util.Try
import scala.jdk.OptionConverters.*

import com.example.jobsboard.config.*
import com.example.jobsboard.logging.syntax.*
import com.stripe.net.Webhook

trait Stripe[F[_]] {
  def createCheckoutSession(jobId: String, userEmail: String): F[Option[Session]]
  def handleWebhook[A](payload: String, signature: String, action: String => F[A]): F[Option[A]]
}

class LiveStripe[F[_]: MonadThrow: Logger] private (stripeConfig: StripeConfig) extends Stripe[F] {
  Stripe_.apiKey = stripeConfig.apiKey

  override def createCheckoutSession(jobId: String, userEmail: String): F[Option[Session]] =
    SessionCreateParams
      .builder()
      .setMode(SessionCreateParams.Mode.PAYMENT)
      .setInvoiceCreation(
        SessionCreateParams.InvoiceCreation
          .builder()
          .setEnabled(true)
          .build()
      )
      .setPaymentIntentData(
        SessionCreateParams.PaymentIntentData
          .builder()
          .setReceiptEmail(userEmail)
          .build()
      )
      .setSuccessUrl(s"${stripeConfig.successUrl}/$jobId")
      .setCancelUrl(stripeConfig.cancelUrl)
      .setCustomerEmail(userEmail)
      .setClientReferenceId(jobId)
      .addLineItem(
        SessionCreateParams.LineItem
          .builder()
          .setQuantity(1L)
          .setPrice(stripeConfig.price)
          .build()
      )
      .build()
      .pure[F]
      .map(params => Session.create(params))
      .map(_.some)
      .logError(err => s"Creating checkout session failed: $err")
      .recover(_ => None)

  override def handleWebhook[A](
      payload: String,
      signature: String,
      action: String => F[A]
  ): F[Option[A]] = {
    MonadThrow[F]
      .fromTry(
        Try(
          Webhook.constructEvent(
            payload,
            signature,
            stripeConfig.webhookSecret
          )
        )
      )
      .logError(_ => "Stripe security verification failed.")
      .flatMap { event =>
        event.getType() match {
          case "checkout.session.completed" =>
            event
              .getDataObjectDeserializer()
              .getObject()
              .toScala
              .map(_.asInstanceOf[Session])
              .flatMap(session => Option(session.getClientReferenceId()))
              .map(action)
              .sequence
              .log(
                {
                  case None    => s"Event ${event.getId()} not producing effect."
                  case Some(v) => s"Event ${event.getId()} was paid."
                },
                err => s"Webhook action failed: ${err.toString}"
              )
          case _ => None.pure[F]
        }
      }
      .recover { case _ => None }
  }

}

object LiveStripe {
  def apply[F[_]: MonadThrow: Logger](stripeConfig: StripeConfig): F[LiveStripe[F]] =
    new LiveStripe[F](stripeConfig).pure[F]
}
