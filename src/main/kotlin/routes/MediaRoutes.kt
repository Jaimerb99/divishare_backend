package com.jrb.routes

import com.jrb.models.GenericResponse
import com.jrb.models.UploadAvatarRequest
import com.jrb.models.UploadReceiptImageRequest
import com.jrb.models.UploadResponse
import com.jrb.services.MediaService
import com.jrb.utils.Constants
import com.jrb.utils.Messages
import com.jrb.utils.getAuthenticatedUserIdOrRespond
import com.jrb.utils.getLanguage
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.Base64

fun Route.mediaRoutes(mediaService: MediaService) {

    authenticate(Constants.Security.AUTH_JWT_NAME) {

        route("/media") {

            post("/upload") {
                val lang = call.getLanguage()

                call.getAuthenticatedUserIdOrRespond(lang) ?: return@post

                val request = try {
                    call.receive<UploadReceiptImageRequest>()
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        GenericResponse(success = false, message = Messages.get(Constants.Messages.INVALID_REQUEST_BODY, lang))
                    )
                    return@post
                }

                try {
                    val fileBytes = Base64.getDecoder().decode(request.base64Image)

                    // Pass the expenseId as the publicId to the service
                    val imageUrl = mediaService.uploadImage(
                        fileBytes = fileBytes,
                        folder = request.folder,
                        publicId = request.expenseId
                    )

                    if (imageUrl != null) {
                        call.respond(
                            HttpStatusCode.OK,
                            UploadResponse(
                                success = true,
                                message = Messages.get(Constants.Messages.IMAGE_UPLOAD_SUCCESS, lang),
                                imageUrl = imageUrl
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            GenericResponse(success = false, message = Messages.get(Constants.Messages.IMAGE_UPLOAD_ERROR, lang))
                        )
                    }

                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        GenericResponse(success = false, message = Messages.get(Constants.Messages.ERROR_BAD_REQUEST, lang))
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        GenericResponse(success = false, message = Messages.get(Constants.Messages.ERROR_INTERNAL_SERVER, lang, e.localizedMessage ?: ""))
                    )
                }
            }

            get("/receipt/{expenseId}") {
                val lang = call.getLanguage()

                call.getAuthenticatedUserIdOrRespond(lang) ?: return@get

                val expenseId = call.parameters["expenseId"]

                if (expenseId.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        GenericResponse(success = false, message = Messages.get(Constants.Messages.ERROR_BAD_REQUEST, lang))
                    )
                    return@get
                }

                // Delegate URL construction to the service
                val fileUrl = mediaService.getReceiptUrl(expenseId)

                // Redirect the client to download the raw bytes directly
                call.respondRedirect(fileUrl, permanent = false)
            }
            post("/avatar/upload") {
                val lang = call.getLanguage()

                // Extract the user ID directly from the token for security.
                // This ensures a user can only update their own profile picture.
                val userId = call.getAuthenticatedUserIdOrRespond(lang) ?: return@post

                val request = try {
                    call.receive<UploadAvatarRequest>()
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        GenericResponse(success = false, message = Messages.get(Constants.Messages.INVALID_REQUEST_BODY, lang))
                    )
                    return@post
                }

                try {
                    val fileBytes = Base64.getDecoder().decode(request.base64Image)

                    // Use the authenticated userId as the publicId
                    val imageUrl = mediaService.uploadImage(
                        fileBytes = fileBytes,
                        folder = Constants.CloudinaryConfig.FOLDER_AVATARS,
                        publicId = userId.toString()
                    )

                    if (imageUrl != null) {
                        call.respond(
                            HttpStatusCode.OK,
                            UploadResponse(
                                success = true,
                                message = Messages.get(Constants.Messages.IMAGE_UPLOAD_SUCCESS, lang),
                                imageUrl = imageUrl
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            GenericResponse(success = false, message = Messages.get(Constants.Messages.IMAGE_UPLOAD_ERROR, lang))
                        )
                    }

                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        GenericResponse(success = false, message = Messages.get(Constants.Messages.ERROR_BAD_REQUEST, lang))
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        GenericResponse(success = false, message = Messages.get(Constants.Messages.ERROR_INTERNAL_SERVER, lang, e.localizedMessage ?: ""))
                    )
                }
            }

            get("/avatar/{targetUserId}") {
                val lang = call.getLanguage()

                // Verify the requester is authenticated
                call.getAuthenticatedUserIdOrRespond(lang) ?: return@get

                // Get the target user ID whose avatar we want to view
                val targetUserId = call.parameters["targetUserId"]

                if (targetUserId.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        GenericResponse(success = false, message = Messages.get(Constants.Messages.ERROR_BAD_REQUEST, lang))
                    )
                    return@get
                }

                // Construct the Cloudinary URL using the target user's ID
                val fileUrl = mediaService.getFileUrl(targetUserId, Constants.CloudinaryConfig.FOLDER_AVATARS)

                // Redirect the client to download the raw bytes
                call.respondRedirect(fileUrl, permanent = false)
            }
        }
    }
}