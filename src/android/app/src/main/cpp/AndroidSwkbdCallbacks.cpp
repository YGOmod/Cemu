#include "AndroidSwkbdCallbacks.h"
#include "JNIUtils.h"

AndroidSwkbdCallbacks::AndroidSwkbdCallbacks()
{
	JNIUtils::ScopedJNIENV env;
	m_emulationActivityClass = JNIUtils::Scopedjclass("info/cemu/Cemu/emulation/EmulationActivity");
	m_showSoftwareKeyboardMethodID = env->GetStaticMethodID(*m_emulationActivityClass, "showEmulationTextInput", "(Ljava/lang/String;I)V");
	m_hideSoftwareKeyboardMethodID = env->GetStaticMethodID(*m_emulationActivityClass, "hideEmulationTextInput", "()V");
}

void AndroidSwkbdCallbacks::showSoftwareKeyboard(const std::string& initialText, sint32 maxLength)
{
	std::thread([&, this]() {
		JNIUtils::ScopedJNIENV env;
		jstring j_initialText = env->NewStringUTF(initialText.c_str());
		JNIUtils::ScopedJNIENV()->CallStaticVoidMethod(*m_emulationActivityClass, m_showSoftwareKeyboardMethodID, j_initialText, maxLength);
		env->DeleteLocalRef(j_initialText);
	}).join();
}

void AndroidSwkbdCallbacks::hideSoftwareKeyboard()
{
	std::thread([this]() {
		JNIUtils::ScopedJNIENV()->CallStaticVoidMethod(*m_emulationActivityClass, m_hideSoftwareKeyboardMethodID);
	}).join();
}