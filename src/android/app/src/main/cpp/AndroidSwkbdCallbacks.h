#pragma once

#include "Cafe/OS/libs/swkbd/swkbd.h"
#include "JNIUtils.h"
class AndroidSwkbdCallbacks : public swkbd::swkbdCallbacks
{
	JNIUtils::Scopedjclass m_emulationActivityClass;
	jmethodID m_showSoftwareKeyboardMethodID;
	jmethodID m_hideSoftwareKeyboardMethodID;

  public:
	AndroidSwkbdCallbacks();
	void showSoftwareKeyboard(const std::string& initialText, sint32 maxLength) override;
	void hideSoftwareKeyboard() override;
};