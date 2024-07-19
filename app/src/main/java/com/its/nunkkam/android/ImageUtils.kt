package com.its.nunkkam.android // 패키지 선언: 이 코드가 속한 패키지를 지정

import android.graphics.Bitmap // 비트맵 이미지 처리 클래스
import android.graphics.BitmapFactory // 비트맵을 디코딩 및 인코딩하기 위한 클래스
import android.graphics.ImageFormat // 이미지 형식 관련 상수
import android.graphics.Rect // 사각형 영역을 정의하는 클래스
import android.graphics.YuvImage // YUV 형식의 이미지 데이터를 다루는 클래스
import androidx.camera.core.ImageProxy // 카메라 이미지 프록시 클래스
import java.io.ByteArrayOutputStream // 바이트 배열 출력 스트림 클래스

// ImageProxy를 Bitmap으로 변환하는 확장 함수 | 이 함수는 YUV 형식의 이미지 데이터를 JPEG로 변환한 후 Bitmap으로 디코딩함
fun ImageProxy.toBufferBitmap(): Bitmap {
    // Y, U, V 버퍼 가져오기
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    // 각 버퍼의 남은 데이터 크기 계산
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    // YUV 데이터를 저장할 바이트 배열 생성
    val nv21 = ByteArray(ySize + uSize + vSize)

    // YUV 데이터 복사
    yBuffer.get(nv21, 0, ySize) // Y 버퍼의 데이터를 nv21 배열의 시작 부분에 복사
    vBuffer.get(nv21, ySize, vSize) // V 버퍼의 데이터를 nv21 배열의 ySize 위치부터 복사
    uBuffer.get(nv21, ySize + vSize, uSize) // U 버퍼의 데이터를 nv21 배열의 ySize + vSize 위치부터 복사

    // YUV 이미지 생성
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    // JPEG 형식으로 압축
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
    val imageBytes = out.toByteArray()
    // 바이트 배열을 Bitmap으로 디코딩하여 반환
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}