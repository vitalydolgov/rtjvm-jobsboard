package com.example.jobsboard.algebra

import cats.implicits.*
import cats.effect.*
import javax.mail.*
import javax.mail.internet.MimeMessage
import java.util.Properties
import org.typelevel.log4cats.Logger

import com.example.jobsboard.config.*

trait Emails[F[_]] {
  def sendEmail(to: String, subject: String, content: String): F[Unit]
  def sendPasswordRecoveryEmail(to: String, token: String): F[Unit]
}

class LiveEmails[F[_]: MonadCancelThrow: Logger] private (config: EmailServiceConfig)
    extends Emails[F] {
  val EmailServiceConfig(host, port, user, password, frontendUrl) = config

  private val propsResource: Resource[F, Properties] = {
    val props = new Properties
    props.put("mail.smtp.auth", true)
    props.put("mail.smtp.starttls.enable", true)
    props.put("mail.smtp.host", host)
    props.put("mail.smtp.port", port)
    props.put("mail.smtp.ssl.trust", host)
    Resource.pure(props)
  }

  private val authResource: Resource[F, Authenticator] =
    Resource.pure(new Authenticator {
      override protected def getPasswordAuthentication(): PasswordAuthentication = {
        new PasswordAuthentication(user, password)
      }
    })

  private def createSession(props: Properties, auth: Authenticator): Resource[F, Session] =
    Resource.pure(Session.getInstance(props, auth))

  private def createMessage(
      session: Session,
      from: String,
      to: String,
      subject: String,
      content: String
  ): Resource[F, MimeMessage] = {
    val message = new MimeMessage(session)
    message.setFrom(from)
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")
    Resource.pure(message)
  }

  override def sendEmail(to: String, subject: String, content: String): F[Unit] = {
    val messageResource = for {
      props <- propsResource
      auth <- authResource
      session <- createSession(props, auth)
      message <- createMessage(session, "jobsboard@example.com", to, subject, content)
    } yield message

    messageResource.use(message => Transport.send(message).pure[F])
  }
  override def sendPasswordRecoveryEmail(to: String, token: String): F[Unit] = {
    val subject = "Jobsboard: Password Recovery"
    val content = s"""
    <div>
      <h1>Jobsboard: Password Recovery</h1>
      <p>Your password recovery token is: $token</p>
      <p>
        Click <a href="$frontendUrl/login">here</a> to get back to the application.
      </p>
      <p>With ❤️ from Jobsboard!</p>
    </div>
    """

    sendEmail(to, subject, content)
  }
}

object LiveEmails {
  def apply[F[_]: MonadCancelThrow: Logger](config: EmailServiceConfig): F[LiveEmails[F]] =
    new LiveEmails[F](config).pure[F]
}
