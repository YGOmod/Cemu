#include "JNIUtils.h"

#include "Cafe/OS/libs/swkbd/swkbd.h"
#include "AndroidSwkbdCallbacks.h"

namespace NativeSwkbd
{
	std::shared_ptr<swkbd::swkbdCallbacks> s_swkbdCallbacks;
	std::string s_currentInputText;
	struct StrDiffs
	{
		size_t newTextStartIndex;
		size_t numberOfCharacterToDelete;
	};
	StrDiffs getStringDiffs(const std::string& newText, const std::string& currentText)
	{
		if (newText.length() < currentText.length() && currentText.starts_with(newText))
			return {newText.length(), currentText.length() - newText.length()};
		if (newText.length() >= currentText.length() && newText.starts_with(currentText))
			return {currentText.length(), 0};
		return {0, currentText.length()};
	}
} // namespace NativeSwkbd

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeSwkbd_initializeSwkbd([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	if (NativeSwkbd::s_swkbdCallbacks != nullptr)
		return;
	NativeSwkbd::s_swkbdCallbacks = std::make_shared<AndroidSwkbdCallbacks>();
	swkbd::setSwkbdCallbacks(NativeSwkbd::s_swkbdCallbacks);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeSwkbd_setCurrentInputText([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jstring initial_text)
{
	NativeSwkbd::s_currentInputText = JNIUtils::JStringToString(env, initial_text);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeSwkbd_onTextChanged([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jstring j_text)
{
	std::string text = JNIUtils::JStringToString(env, j_text);
	auto stringDiff = NativeSwkbd::getStringDiffs(text, NativeSwkbd::s_currentInputText);
	for (size_t i = 0; i < stringDiff.numberOfCharacterToDelete; i++)
		swkbd::keyInput(swkbd::BACKSPACE_KEYCODE);
	for (size_t i = stringDiff.newTextStartIndex; i < text.length(); i++)
		swkbd::keyInput(text.at(i));
	NativeSwkbd::s_currentInputText = std::move(text);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeSwkbd_onFinishedInputEdit([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	swkbd::keyInput(swkbd::RETURN_KEYCODE);
}