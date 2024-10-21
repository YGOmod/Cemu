#include "JNIUtils.h"
#include "AndroidAudio.h"
#include "AndroidEmulatedController.h"
#include "AndroidFilesystemCallbacks.h"
#include "Cafe/HW/Latte/Core/LatteOverlay.h"
#include "Cafe/HW/Latte/Renderer/Vulkan/VulkanAPI.h"
#include "Cafe/HW/Latte/Renderer/Vulkan/VulkanRenderer.h"
#include "Cafe/CafeSystem.h"
#include "Cemu/GuiSystem/GuiSystem.h"
#include "GameTitleLoader.h"
#include "input/ControllerFactory.h"
#include "input/InputManager.h"
#include "input/api/Android/AndroidController.h"
#include "input/api/Android/AndroidControllerProvider.h"
#include "config/ActiveSettings.h"
#include "Cemu/ncrypto/ncrypto.h"

// forward declaration from main.cpp
void CemuCommonInit();

namespace NativeEmulation
{
	void initializeAudioDevices()
	{
		auto& config = g_config.data();
		if (!config.tv_device.empty())
			AndroidAudio::createAudioDevice(IAudioAPI::AudioAPI::Cubeb, config.tv_channels, config.tv_volume, true);

		if (!config.pad_device.empty())
			AndroidAudio::createAudioDevice(IAudioAPI::AudioAPI::Cubeb, config.pad_channels, config.pad_volume, false);
	}

	void createCemuDirectories()
	{
		std::wstring mlc = ActiveSettings::GetMlcPath().generic_wstring();

		// create sys/usr folder in mlc01
		const auto sysFolder = fs::path(mlc).append(L"sys");
		fs::create_directories(sysFolder);

		const auto usrFolder = fs::path(mlc).append(L"usr");
		fs::create_directories(usrFolder);
		fs::create_directories(fs::path(usrFolder).append("title/00050000")); // base
		fs::create_directories(fs::path(usrFolder).append("title/0005000c")); // dlc
		fs::create_directories(fs::path(usrFolder).append("title/0005000e")); // update

		// Mii Maker save folders {0x500101004A000, 0x500101004A100, 0x500101004A200},
		fs::create_directories(fs::path(mlc).append(L"usr/save/00050010/1004a000/user/common/db"));
		fs::create_directories(fs::path(mlc).append(L"usr/save/00050010/1004a100/user/common/db"));
		fs::create_directories(fs::path(mlc).append(L"usr/save/00050010/1004a200/user/common/db"));

		// lang files
		auto langDir = fs::path(mlc).append(L"sys/title/0005001b/1005c000/content");
		fs::create_directories(langDir);

		auto langFile = fs::path(langDir).append("language.txt");
		if (!fs::exists(langFile))
		{
			std::ofstream file(langFile);
			if (file.is_open())
			{
				const char* langStrings[] = {"ja", "en", "fr", "de", "it", "es", "zh", "ko", "nl", "pt", "ru", "zh"};
				for (const char* lang : langStrings)
					file << fmt::format(R"("{}",)", lang) << std::endl;

				file.flush();
				file.close();
			}
		}

		auto countryFile = fs::path(langDir).append("country.txt");
		if (!fs::exists(countryFile))
		{
			std::ofstream file(countryFile);
			for (sint32 i = 0; i < 201; i++)
			{
				const char* countryCode = NCrypto::GetCountryAsString(i);
				if (boost::iequals(countryCode, "NN"))
					file << "NULL," << std::endl;
				else
					file << fmt::format(R"("{}",)", countryCode) << std::endl;
			}
			file.flush();
			file.close();
		}

		// cemu directories
		const auto controllerProfileFolder = ActiveSettings::GetConfigPath(L"controllerProfiles").generic_wstring();
		if (!fs::exists(controllerProfileFolder))
			fs::create_directories(controllerProfileFolder);

		const auto memorySearcherFolder = ActiveSettings::GetUserDataPath(L"memorySearcher").generic_wstring();
		if (!fs::exists(memorySearcherFolder))
			fs::create_directories(memorySearcherFolder);
	}
	enum StartGameResult : sint32
	{
		SUCCESSFUL = 0,
		ERROR_GAME_BASE_FILES_NOT_FOUND = 1,
		ERROR_NO_DISC_KEY = 2,
		ERROR_NO_TITLE_TIK = 3,
		ERROR_UNKNOWN = 4,
	};
	StartGameResult startGame(const fs::path& launchPath)
	{
		TitleInfo launchTitle{launchPath};
		if (launchTitle.IsValid())
		{
			// the title might not be in the TitleList, so we add it as a temporary entry
			CafeTitleList::AddTitleFromPath(launchPath);
			// title is valid, launch from TitleId
			TitleId baseTitleId;
			if (!CafeTitleList::FindBaseTitleId(launchTitle.GetAppTitleId(), baseTitleId))
			{
				return ERROR_GAME_BASE_FILES_NOT_FOUND;
			}
			CafeSystem::STATUS_CODE r = CafeSystem::PrepareForegroundTitle(baseTitleId);
			if (r != CafeSystem::STATUS_CODE::SUCCESS)
			{
				return ERROR_UNKNOWN;
			}
		}
		else // if (launchTitle.GetFormat() == TitleInfo::TitleDataFormat::INVALID_STRUCTURE )
		{
			// title is invalid, if it's an RPX/ELF we can launch it directly
			// otherwise it's an error
			CafeTitleFileType fileType = DetermineCafeSystemFileType(launchPath);
			if (fileType == CafeTitleFileType::RPX || fileType == CafeTitleFileType::ELF)
			{
				CafeSystem::STATUS_CODE r = CafeSystem::PrepareForegroundTitleFromStandaloneRPX(launchPath);
				if (r != CafeSystem::STATUS_CODE::SUCCESS)
				{
					return ERROR_UNKNOWN;
				}
			}
			else if (launchTitle.GetInvalidReason() == TitleInfo::InvalidReason::NO_DISC_KEY)
			{
				return ERROR_NO_DISC_KEY;
			}
			else if (launchTitle.GetInvalidReason() == TitleInfo::InvalidReason::NO_TITLE_TIK)
			{
				return ERROR_NO_TITLE_TIK;
			}
			else
			{
				return ERROR_UNKNOWN;
			}
		}
		CafeSystem::LaunchForegroundTitle();
		return SUCCESSFUL;
	}
} // namespace NativeEmulation

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeEmulation_setReplaceTVWithPadView([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean swapped)
{
	// Emulate pressing the TAB key for showing DRC instead of TV
	GuiSystem::getWindowInfo().set_keystate(GuiSystem::PlatformKeyCodes::TAB, swapped);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeEmulation_initializeActiveSettings(JNIEnv* env, [[maybe_unused]] jclass clazz, jstring data_path, jstring cache_path)
{
	std::string dataPath = JNIUtils::JStringToString(env, data_path);
	std::string cachePath = JNIUtils::JStringToString(env, cache_path);
	std::set<fs::path> failedWriteAccess;
	ActiveSettings::SetPaths(false, {}, dataPath, dataPath, cachePath, dataPath, failedWriteAccess);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeEmulation_initializeEmulation([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	FilesystemAndroid::setFilesystemCallbacks(std::make_shared<AndroidFilesystemCallbacks>());
	g_config.SetFilename(ActiveSettings::GetConfigPath("settings.xml").generic_wstring());
	NativeEmulation::createCemuDirectories();
	NetworkConfig::LoadOnce();
	ActiveSettings::Init();
	LatteOverlay_init();
	CemuCommonInit();
	InitializeGlobalVulkan();
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeEmulation_initializerRenderer(JNIEnv* env, [[maybe_unused]] jclass clazz, jobject testSurface)
{
	JNIUtils::handleNativeException(env, [&]() {
		cemu_assert_debug(testSurface != nullptr);
		// TODO: cleanup surface
		GuiSystem::getWindowInfo().window_main.surface = ANativeWindow_fromSurface(env, testSurface);
		g_renderer = std::make_unique<VulkanRenderer>();
	});
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeEmulation_setDPI([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jfloat dpi)
{
	auto& windowInfo = GuiSystem::getWindowInfo();
	windowInfo.dpi_scale = windowInfo.pad_dpi_scale = dpi;
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeEmulation_clearSurface([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean is_main_canvas)
{
	if (!is_main_canvas)
	{
		auto renderer = static_cast<VulkanRenderer*>(g_renderer.get());
		if (renderer)
			renderer->StopUsingPadAndWait();
	}
}
extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeEmulation_recreateRenderSurface([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jboolean is_main_canvas)
{
	// TODO
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeEmulation_setSurface(JNIEnv* env, [[maybe_unused]] jclass clazz, jobject surface, jboolean is_main_canvas)
{
	JNIUtils::handleNativeException(env, [&]() {
		cemu_assert_debug(surface != nullptr);
		auto& windowHandleInfo = is_main_canvas ? GuiSystem::getWindowInfo().canvas_main : GuiSystem::getWindowInfo().canvas_pad;
		if (windowHandleInfo.surface)
		{
			ANativeWindow_release(static_cast<ANativeWindow*>(windowHandleInfo.surface));
			windowHandleInfo.surface = nullptr;
		}
		windowHandleInfo.surface = ANativeWindow_fromSurface(env, surface);
		int width, height;
		if (is_main_canvas)
			GuiSystem::getWindowPhysSize(width, height);
		else
			GuiSystem::getPadWindowPhysSize(width, height);
		VulkanRenderer::GetInstance()->InitializeSurface({width, height}, is_main_canvas);
	});
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeEmulation_setSurfaceSize([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jint width, jint height, jboolean is_main_canvas)
{
	auto& windowInfo = GuiSystem::getWindowInfo();
	if (is_main_canvas)
	{
		windowInfo.width = windowInfo.phys_width = width;
		windowInfo.height = windowInfo.phys_height = height;
	}
	else
	{
		windowInfo.pad_width = windowInfo.phys_pad_width = width;
		windowInfo.pad_height = windowInfo.phys_pad_height = height;
	}
}

extern "C" [[maybe_unused]] JNIEXPORT jint JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeEmulation_startGame([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jstring launchPath)
{
	GuiSystem::getWindowInfo().set_keystates_up();
	NativeEmulation::initializeAudioDevices();
	return NativeEmulation::startGame(JNIUtils::JStringToString(env, launchPath));
}