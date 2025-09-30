package com.ferji.inspecciones.data.network.sendgrid


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST



interface SendGridApiService  {

    @POST("v3/mail/send")
    suspend fun sendEmail(@Body mailData: SendGridMail): Response<Unit>

}
