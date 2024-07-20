안구 건조증 예방 프로젝트 성공 기원


김정연 24/07/19
버튼 누르면 히스토리 그래프 나오게 연동했습니다.


1. ChartActivity가 추가되었습니다.
2. MainActivity에 ChartActivity를 시작하는 버튼과 함수가 추가되었습니다.
3. build.gradle (:app) 파일에 AnyChart 라이브러리 의존성이 추가되었습니다.
4. AndroidManifest.xml에 ChartActivity가 등록되었습니다.
5. 모든 support 라이브러리와  Android간의 충돌을 해결하기 위해 Migrate to AndroidX하는 과정에서 Refactor메뉴에서 Migrate to AndroidX을 못 찾아서 수동으로 마이그레이션했습니다.


-gradle.properties 파일 수정:

이 파일에 다음 두 줄을 추가했습니다:
Copyandroid.useAndroidX=true
android.enableJetifier=true

-build.gradle (프로젝트 수준) 파일 수정:

Google 서비스 플러그인 버전을 최신 버전으로 업데이트했습니다:
gradleCopybuildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.2'
    }
}

-build.gradle (앱 수준) 파일 수정:

 모든 support 라이브러리 의존성을 AndroidX 의존성으로 교체했습니다.
 예를 들어:
gradleCopyimplementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'com.google.android.material:material:1.9.0'

-코드 내 import 문 수정:

모든 파일에서 'android.support' 패키지를 'androidx' 패키지로 변경했습니다. 예를 들어:
kotlinCopyimport androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

-XML 레이아웃 파일 수정:

 레이아웃 파일에서 support 라이브러리 클래스를 AndroidX 클래스로 변경했습니다. 예를 들어:
xmlCopy<androidx.constraintlayout.widget.ConstraintLayout
    ...>
</androidx.constraintlayout.widget.ConstraintLayout>

-AndroidManifest.xml 파일 수정:

매니페스트 파일에서 support 라이브러리 참조를 AndroidX 참조로 변경했습니다.

-프로젝트 동기화 및 빌드:

변경 사항을 적용한 후, "Sync Project with Gradle Files"를 실행하고 프로젝트를 다시 빌드했습니다.

-오류 해결:

빌드 과정에서 발생한 오류를 하나씩 해결했습니다. 대부분 클래스나 패키지 이름 변경과 관련된 오류였을 것입니다.

-라이브러리 버전 확인:

사용 중인 모든 라이브러리가 AndroidX와 호환되는 버전인지 확인하고, 필요한 경우 업데이트했습니다.

-테스트:
앱의 모든 기능을 테스트하여 마이그레이션으로 인한 문제가 없는지 확인했습니다

------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
