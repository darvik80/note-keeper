package xyz.crearts.notekeeper.data.remote.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming
import xyz.crearts.notekeeper.data.model.Attachment

interface AttachmentApiService {

    @Multipart
    @POST("attachments/upload")
    suspend fun uploadAttachment(
        @Part file: MultipartBody.Part,
        @Part("parentId") parentId: RequestBody,
        @Part("parentType") parentType: RequestBody
    ): Response<Attachment>

    @DELETE("attachments/{attachmentId}")
    suspend fun deleteAttachment(@Path("attachmentId") attachmentId: String): Response<Unit>

    @Streaming
    @GET("attachments/{attachmentId}/download")
    suspend fun downloadAttachment(@Path("attachmentId") attachmentId: String): Response<okhttp3.ResponseBody>
}
