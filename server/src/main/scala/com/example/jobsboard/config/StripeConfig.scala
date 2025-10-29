package com.example.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class StripeConfig(
    apiKey: String,
    price: String,
    successUrl: String,
    cancelUrl: String,
    webhookSecret: String
) derives ConfigReader
