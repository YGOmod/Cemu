#include "Cafe/CafeSystem.h"
#include "config/CemuConfig.h"
#include "Cafe/GraphicPack/GraphicPack2.h"
#include "JNIUtils.h"

namespace NativeGraphicPacks
{
	std::unordered_map<sint64, GraphicPackPtr> s_graphicPacks;

	void fillGraphicPacks()
	{
		s_graphicPacks.clear();
		auto graphicPacks = GraphicPack2::GetGraphicPacks();
		for (auto&& graphicPack : graphicPacks)
		{
			s_graphicPacks[reinterpret_cast<sint64>(graphicPack.get())] = graphicPack;
		}
	}

	void saveGraphicPackStateToConfig(GraphicPackPtr graphicPack)
	{
		auto& data = g_config.data();
		auto filename = _utf8ToPath(graphicPack->GetNormalizedPathString());
		if (data.graphic_pack_entries.contains(filename))
			data.graphic_pack_entries.erase(filename);
		if (graphicPack->IsEnabled())
		{
			data.graphic_pack_entries.try_emplace(filename);
			auto& it = data.graphic_pack_entries[filename];
			// otherwise store all selected presets
			for (const auto& preset : graphicPack->GetActivePresets())
				it.try_emplace(preset->category, preset->name);
		}
		else if (graphicPack->IsDefaultEnabled())
		{
			// save that its disabled
			data.graphic_pack_entries.try_emplace(filename);
			auto& it = data.graphic_pack_entries[filename];
			it.try_emplace("_disabled", "false");
		}
		g_config.Save();
	}

	jobject getGraphicPresets(JNIEnv* env, GraphicPackPtr graphicPack, sint64 id)
	{
		auto graphicPackPresetClass = env->FindClass("info/cemu/Cemu/nativeinterface/NativeGraphicPacks$GraphicPackPreset");
		auto graphicPackPresetCtorId = env->GetMethodID(graphicPackPresetClass, "<init>", "(JLjava/lang/String;Ljava/util/ArrayList;Ljava/lang/String;)V");

		std::vector<std::string> order;
		auto presets = graphicPack->GetCategorizedPresets(order);

		std::vector<jobject> presetsJobjects;
		for (const auto& category : order)
		{
			const auto& entry = presets[category];
			// test if any preset is visible and update its status
			if (std::none_of(entry.cbegin(), entry.cend(), [graphicPack](const auto& p) { return p->visible; }))
			{
				continue;
			}

			jstring categoryJStr = category.empty() ? nullptr : env->NewStringUTF(category.c_str());
			std::vector<std::string> presetSelections;
			std::optional<std::string> activePreset;
			for (auto& pentry : entry)
			{
				if (!pentry->visible)
					continue;

				presetSelections.push_back(pentry->name);

				if (pentry->active)
					activePreset = pentry->name;
			}

			jstring activePresetJstr = nullptr;
			if (activePreset)
				activePresetJstr = env->NewStringUTF(activePreset->c_str());
			else if (!presetSelections.empty())
				activePresetJstr = env->NewStringUTF(presetSelections.front().c_str());
			auto presetJObject = env->NewObject(graphicPackPresetClass, graphicPackPresetCtorId, id, categoryJStr, JNIUtils::createJavaStringArrayList(env, presetSelections), activePresetJstr);
			presetsJobjects.push_back(presetJObject);
		}
		return JNIUtils::createArrayList(env, presetsJobjects);
	}
} // namespace NativeGraphicPacks

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeGraphicPacks_refreshGraphicPacks([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	if (!CafeSystem::IsTitleRunning())
	{
		GraphicPack2::ClearGraphicPacks();
		GraphicPack2::LoadAll();
		NativeGraphicPacks::fillGraphicPacks();
	}
}

extern "C" [[maybe_unused]] JNIEXPORT jobject JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeGraphicPacks_getGraphicPackBasicInfos(JNIEnv* env, [[maybe_unused]] jclass clazz)
{
	auto graphicPackInfoClass = env->FindClass("info/cemu/Cemu/nativeinterface/NativeGraphicPacks$GraphicPackBasicInfo");
	auto graphicPackInfoCtorId = env->GetMethodID(graphicPackInfoClass, "<init>", "(JLjava/lang/String;Ljava/util/ArrayList;)V");

	std::vector<jobject> graphicPackInfoJObjects;
	for (auto&& graphicPack : NativeGraphicPacks::s_graphicPacks)
	{
		jstring virtualPath = env->NewStringUTF(graphicPack.second->GetVirtualPath().c_str());
		jlong id = graphicPack.first;
		jobject titleIds = JNIUtils::createJavaLongArrayList(env, graphicPack.second->GetTitleIds());
		jobject jGraphicPack = env->NewObject(graphicPackInfoClass, graphicPackInfoCtorId, id, virtualPath, titleIds);
		graphicPackInfoJObjects.push_back(jGraphicPack);
	}
	return JNIUtils::createArrayList(env, graphicPackInfoJObjects);
}

extern "C" JNIEXPORT jobject JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeGraphicPacks_getGraphicPack(JNIEnv* env, [[maybe_unused]] jclass clazz, jlong id)
{
	auto graphicPackClass = env->FindClass("info/cemu/Cemu/nativeinterface/NativeGraphicPacks$GraphicPack");
	auto graphicPackCtorId = env->GetMethodID(graphicPackClass, "<init>", "(JZLjava/lang/String;Ljava/lang/String;Ljava/util/ArrayList;)V");
	auto graphicPack = NativeGraphicPacks::s_graphicPacks.at(id);

	jstring graphicPackName = env->NewStringUTF(graphicPack->GetName().c_str());
	jstring graphicPackDescription = env->NewStringUTF(graphicPack->GetDescription().c_str());
	return env->NewObject(
		graphicPackClass,
		graphicPackCtorId,
		id,
		graphicPack->IsEnabled(),
		graphicPackName,
		graphicPackDescription,
		NativeGraphicPacks::getGraphicPresets(env, graphicPack, id));
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeGraphicPacks_setGraphicPackActive([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jlong id, jboolean active)
{
	auto graphicPack = NativeGraphicPacks::s_graphicPacks.at(id);
	graphicPack->SetEnabled(active);
	NativeGraphicPacks::saveGraphicPackStateToConfig(graphicPack);
}

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeGraphicPacks_setGraphicPackActivePreset([[maybe_unused]] JNIEnv* env, [[maybe_unused]] jclass clazz, jlong id, jstring category, jstring preset)
{
	std::string presetCategory = category == nullptr ? "" : JNIUtils::JStringToString(env, category);
	auto graphicPack = NativeGraphicPacks::s_graphicPacks.at(id);
	graphicPack->SetActivePreset(presetCategory, JNIUtils::JStringToString(env, preset));
	NativeGraphicPacks::saveGraphicPackStateToConfig(graphicPack);
}

extern "C" [[maybe_unused]] JNIEXPORT jobject JNICALL
Java_info_cemu_Cemu_nativeinterface_NativeGraphicPacks_getGraphicPackPresets(JNIEnv* env, [[maybe_unused]] jclass clazz, jlong id)
{
	return NativeGraphicPacks::getGraphicPresets(env, NativeGraphicPacks::s_graphicPacks.at(id), id);
}