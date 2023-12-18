package com.example.znajdzlek
import android.accounts.NetworkErrorException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST


data class MedicationRequest(val medication: String)

data class MedicationResponse(val result: String)
interface MedicationApiService {
    @POST("/api/medication")
    fun getMedicationData(@Body request: MedicationRequest): Call<MedicationResponse>
}

class MedicationApiClient {
    companion object {
        private const val BASE_URL = "http://185.47.65.159:5000/api/medication/"

        fun create(): MedicationApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(MedicationApiService::class.java)
        }
    }
}

sealed class Result<out T : Any> {
    data class Success<out T : Any>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}


class APIDataFetcher {
    fun getMedicationData(name: String): Result<MedicationResponse> {
        val medicationApiService = MedicationApiClient.create()
        val request = MedicationRequest(name)

        val call = medicationApiService.getMedicationData(request)
        return try {
            val response = call.execute()
            if (response.isSuccessful) {
                println(">>>successful response")
                val medicationData = response.body()
                Result.Success(medicationData!!)
            } else {
                println(">>>Unusccessful response ${response.code()}")
                Result.Error(Exception("Unsuccessful response: ${response.code()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println(">>>ERROR occured ${e.message}")
            Result.Error(e)
        }
    }

}