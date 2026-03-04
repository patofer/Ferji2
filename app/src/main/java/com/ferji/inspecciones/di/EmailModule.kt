package com.ferji.inspecciones.di

import com.ferji.inspecciones.utils.email.EmailService
import com.ferji.inspecciones.utils.email.SmtpEmailService
// import com.ferji.inspecciones.utils.email.SendGridEmailService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt para proveer la implementación de [EmailService].
 *
 * Para cambiar de proveedor de email, solo cambia el @Binds:
 *
 * ● JavaMail/SMTP (actual, gratuito):
 *   @Binds abstract fun bindEmailService(impl: SmtpEmailService): EmailService
 *
 * ● SendGrid (API REST, de pago):
 *   @Binds abstract fun bindEmailService(impl: SendGridEmailService): EmailService
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EmailModule {

    @Binds
    @Singleton
    abstract fun bindEmailService(impl: SmtpEmailService): EmailService
}

