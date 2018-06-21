package com.anb.kotlinemojify

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.widget.Toast
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import timber.log.Timber

object Emojifier {

    private val EMOJI_SCALE_FACTOR = .9f
    private val SMILING_PROB_THRESHOLD = .15
    private val EYE_OPEN_PROB_THRESHOLD = .5

    fun detectFacesandOverlayEmoji(context: Context, picture: Bitmap): Bitmap {

        // Create the face detector, disable tracking and enable classifications
        val detector = FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build()

        // Build the frame
        val frame = Frame.Builder().setBitmap(picture).build()

        // Detect the faces
        val faces = detector.detect(frame)

        // Log the number of faces
        Timber.d("detectFaces: number of faces = " + faces.size())

        // Initialize result bitmap to original picture
        var resultBitmap = picture

        // If there are no faces detected, show a Toast message
        if (faces.size() == 0) {
            Toast.makeText(context, R.string.no_faces_message, Toast.LENGTH_SHORT).show()
        } else {

            // Iterate through the faces
            for (i in 0 until faces.size()) {
                val face = faces.valueAt(i)

                val emojiBitmap: Bitmap?
                when (whichEmoji(face)) {
                    Emojifier.Emoji.SMILE -> emojiBitmap = BitmapFactory.decodeResource(context.resources,
                            R.drawable.smile)
                    Emojifier.Emoji.FROWN -> emojiBitmap = BitmapFactory.decodeResource(context.resources,
                            R.drawable.frown)
                    Emojifier.Emoji.LEFT_WINK -> emojiBitmap = BitmapFactory.decodeResource(context.resources,
                            R.drawable.leftwink)
                    Emojifier.Emoji.RIGHT_WINK -> emojiBitmap = BitmapFactory.decodeResource(context.resources,
                            R.drawable.rightwink)
                    Emojifier.Emoji.LEFT_WINK_FROWN -> emojiBitmap = BitmapFactory.decodeResource(context.resources,
                            R.drawable.leftwinkfrown)
                    Emojifier.Emoji.RIGHT_WINK_FROWN -> emojiBitmap = BitmapFactory.decodeResource(context.resources,
                            R.drawable.rightwinkfrown)
                    Emojifier.Emoji.CLOSED_EYE_SMILE -> emojiBitmap = BitmapFactory.decodeResource(context.resources,
                            R.drawable.closed_smile)
                    Emojifier.Emoji.CLOSED_EYE_FROWN -> emojiBitmap = BitmapFactory.decodeResource(context.resources,
                            R.drawable.closed_frown)
                    else -> {
                        emojiBitmap = null
                        Toast.makeText(context, R.string.no_emoji, Toast.LENGTH_SHORT).show()
                    }
                }

                // Add the emojiBitmap to the proper position in the original image
                resultBitmap = addBitmapToFace(resultBitmap, emojiBitmap!!, face)
            }
        }


        // Release the detector
        detector.release()

        return resultBitmap
    }

    private fun whichEmoji(face: Face): Emoji {
        // Log all the probabilities
        Timber.d("whichEmoji: smilingProb = " + face.isSmilingProbability)
        Timber.d("whichEmoji: leftEyeOpenProb = " + face.isLeftEyeOpenProbability)
        Timber.d("whichEmoji: rightEyeOpenProb = " + face.isRightEyeOpenProbability)


        val smiling = face.isSmilingProbability > SMILING_PROB_THRESHOLD

        val leftEyeClosed = face.isLeftEyeOpenProbability < EYE_OPEN_PROB_THRESHOLD
        val rightEyeClosed = face.isRightEyeOpenProbability < EYE_OPEN_PROB_THRESHOLD


        // Determine and log the appropriate emoji
        val emoji: Emoji
        if (smiling) {
            if (leftEyeClosed && !rightEyeClosed) {
                emoji = Emoji.LEFT_WINK
            } else if (rightEyeClosed && !leftEyeClosed) {
                emoji = Emoji.RIGHT_WINK
            } else if (leftEyeClosed) {
                emoji = Emoji.CLOSED_EYE_SMILE
            } else {
                emoji = Emoji.SMILE
            }
        } else {
            if (leftEyeClosed && !rightEyeClosed) {
                emoji = Emoji.LEFT_WINK_FROWN
            } else if (rightEyeClosed && !leftEyeClosed) {
                emoji = Emoji.RIGHT_WINK_FROWN
            } else if (leftEyeClosed) {
                emoji = Emoji.CLOSED_EYE_FROWN
            } else {
                emoji = Emoji.FROWN
            }
        }


        // Log the chosen Emoji
        Timber.d("whichEmoji: " + emoji.name)

        // return the chosen Emoji
        return emoji
    }

    private fun addBitmapToFace(backgroundBitmap: Bitmap, emojiBitmap: Bitmap, face: Face): Bitmap {
        var emojiBitmap = emojiBitmap

        // Initialize the results bitmap to be a mutable copy of the original image
        val resultBitmap = Bitmap.createBitmap(backgroundBitmap.width,
                backgroundBitmap.height, backgroundBitmap.config)

        // Scale the emoji so it looks better on the face
        val scaleFactor = EMOJI_SCALE_FACTOR

        // Determine the size of the emoji to match the width of the face and preserve aspect ratio
        val newEmojiWidth = (face.width * scaleFactor).toInt()
        val newEmojiHeight = (emojiBitmap.height * newEmojiWidth / emojiBitmap.width * scaleFactor).toInt()


        // Scale the emoji
        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false)

        // Determine the emoji position so it best lines up with the face
        val emojiPositionX = face.position.x + face.width / 2 - emojiBitmap.width / 2
        val emojiPositionY = face.position.y + face.height / 2 - emojiBitmap.height / 3

        // Create the canvas and draw the bitmaps to it
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null)

        return resultBitmap
    }


    // Enum for all possible Emojis
    private enum class Emoji {
        SMILE,
        FROWN,
        LEFT_WINK,
        RIGHT_WINK,
        LEFT_WINK_FROWN,
        RIGHT_WINK_FROWN,
        CLOSED_EYE_SMILE,
        CLOSED_EYE_FROWN
    }
}