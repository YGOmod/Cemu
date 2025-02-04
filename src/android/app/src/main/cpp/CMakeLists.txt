add_library(CemuAndroid SHARED
        AndroidAudio.cpp
        AndroidEmulatedController.cpp
        AndroidSwkbdCallbacks.cpp
        CMakeLists.txt
        GameTitleLoader.cpp
        Image.cpp
        JNIUtils.cpp
        NativeEmulation.cpp
        NativeGameTitles.cpp
        NativeGraphicPacks.cpp
        NativeInput.cpp
        NativeLib.cpp
        NativeSettings.cpp
        NativeSwkbd.cpp
)

target_link_libraries(CemuAndroid PRIVATE
        -landroid
        CemuCommon
        CemuAudio
        CemuComponents
        CemuCafe
        CemuBin
)
