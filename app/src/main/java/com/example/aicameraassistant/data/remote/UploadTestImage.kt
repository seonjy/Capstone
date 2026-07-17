package com.example.aicameraassistant.data.remote

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

// drawable/testimage를 서버로 업로드하는 테스트 함수
private fun uploadTestImage(
    context: Context,
    onResult: (String) -> Unit
) {
    val url = "https://lamprophonic-unclosable-maryellen.ngrok-free.dev/upload"

    try {
        // drawable 안의 testimage 찾기
        val resourceId = context.resources.getIdentifier(
            "testimage",
            "drawable",
            context.packageName
        )

        if (resourceId == 0) {
            onResult("drawable/testimage 파일을 찾을 수 없음")
            return
        }

        val inputStream = context.resources.openRawResource(resourceId)

        // 캐시 디렉토리에 임시 파일로 저장 후 업로드
        val tempFile = File(context.cacheDir, "testimage.jpg")
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }

        val client = OkHttpClient()


        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("user_id", "test_user_001")
            .addFormDataPart(
                "file",
                "testimage.jpg",
                tempFile.asRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()


        // 네트워크 요청은 백그라운드에서 실행(메인스레드가 아닌 별도 스레드에서 실행)
        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        onResult("실패: HTTP ${response.code}")
                        return@use
                    }

                    // JSON 응답에서 ok 값 확인
                    val ok = try {
                        JSONObject(body).optBoolean("ok", false)
                    } catch (e: Exception) {
                        false
                    }

                    onResult(
                        if (ok) "성공(ok=true)"
                        else "응답은 왔는데 ok=false"
                    )
                }
            } catch (e: Exception) {
                onResult("실패: ${e.message}")
            }
        }.start()

    } catch (e: Exception) {
        onResult("로컬 이미지 읽기 실패: ${e.message}")
    }
}
