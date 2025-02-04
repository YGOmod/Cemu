#pragma once

#include <boost/iostreams/device/file_descriptor.hpp>
#include <boost/iostreams/stream_buffer.hpp>

#include "Common/unix/FilesystemAndroid.h"

using fd_streambuf = boost::iostreams::stream_buffer<boost::iostreams::file_descriptor_source>;

class ContentUriStream : public fd_streambuf, public std::istream
{
  public:
	explicit ContentUriStream(const std::filesystem::path& path)
		: fd_streambuf(FilesystemAndroid::openContentUri(path), boost::iostreams::close_handle), std::istream(this) {}

	bool is_open()
	{
		return component()->is_open();
	}
};