/* Tencent is pleased to support the open source community by making Hippy available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.mtt.hippy.bridge.libraryloader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;

import com.tencent.mtt.hippy.adapter.soloader.HippySoLoaderAdapter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


/**
 * FileName: SoLoaderNew
 * Description：
 * History：
 */
public class LibraryLoader
{

	private static final ArrayList<String>	SO_LIST;
	private static final long				NO_VALUE					= -1L;
	/**
	 * Size of reading buffers.
	 */
	private static final int				BUFFER_SIZE					= 0x4000;
	private static SharedPreferences		sSharedPreferences;
	private static String					DEFAULT_LIBRARY_DIR;
	private static String					PRIVATE_LIBRARY_DIR;
	private static ArrayList<String>		SO_LOADED_LIST				= new ArrayList<>();

	private static HippySoLoaderAdapter		sHippySoLoaderAdapter;
	static
	{
		SO_LIST = new ArrayList<String>();
//		SO_LIST.add("libmtt_shared.so");
//		SO_LIST.add("libmttv8.so");
//		SO_LIST.add("libhippybridge.so");
//		SO_LIST.add("libflexbox.so");
	}

	public static void init(Context context,  SharedPreferences sharedPreferences,
			HippySoLoaderAdapter hippySoLoaderAdapter)
	{
		try
		{
			sSharedPreferences = sharedPreferences;
			sHippySoLoaderAdapter = hippySoLoaderAdapter;
			ApplicationInfo applicationInfo = context.getApplicationInfo();
			DEFAULT_LIBRARY_DIR = applicationInfo.nativeLibraryDir;
			PRIVATE_LIBRARY_DIR = applicationInfo.dataDir + File.separator + "private_hy_libs";
		}
		catch (Throwable e)
		{
		}
	}

	public static synchronized void loadLibraryIfNeed(String shortName)
	{

		String libraryName = mapLibraryName(shortName);
		boolean isHippySo = SO_LIST.contains(libraryName);
		if (true || isHippySo && SO_LOADED_LIST.contains(libraryName))
		{
			return;
		}
		if (isHippySo)
		{
			String currentName = "";
			try
			{
				for (String name : SO_LIST)
				{
					if (SO_LOADED_LIST.contains(name))
					{
						continue;
					}
					currentName = name;
					loadLibraryBySoName(name);
					SO_LOADED_LIST.add(currentName);
				}
			}
			catch (Throwable e)
			{
				throw e;
			}
		}
		else
		{
			try
			{
				loadLibraryBySoName(libraryName);
			}
			catch (Throwable e)
			{

			}
		}
	}

	private static void loadLibraryBySoName(String libraryName)
	{

		//优先走自己定义的
		if (loadFormCusPath(libraryName))
		{
			return;
		}


		//然后先从系统目录中去加载so
		boolean flag = loadFormDefaultDir(libraryName);
		if (!flag)
		{
			flag = loadFormPrivateDir(libraryName);
			if (!flag)
			{
				String shortName = libraryName.substring(3, libraryName.length() - 3);
				System.loadLibrary(shortName);
			}
		}
	}

	private static boolean loadFormCusPath(String libraryName)
	{
		String cusSoPath = sHippySoLoaderAdapter.loadSoPath(libraryName);

		if (TextUtils.isEmpty(cusSoPath))
		{
			return false;
		}
		try
		{
			File file = new File(cusSoPath);
			if (file != null && file.exists())
			{
				System.load(file.getAbsolutePath());
				return true;
			}
		}
		catch (Throwable e)
		{
		}
		return false;

	}

	private static boolean loadFormDefaultDir(String libraryName)
	{
		if (TextUtils.isEmpty(DEFAULT_LIBRARY_DIR))
		{
			return false;
		}
		try
		{
			File file = new File(DEFAULT_LIBRARY_DIR, libraryName);
			if (file != null && file.exists())
			{
				System.load(file.getAbsolutePath());
				return true;
			}
		}
		catch (Throwable e)
		{
		}
		return false;
	}

  private static boolean loadFormPrivateDir(String libraryName) {
	  return false;
  }

	/**
	 * Returns the platform specific file name format for the shared library
	 * named by the argument. On Android, this would turn {@code "MyLibrary"}
	 * into {@code "libMyLibrary.so"}.
	 */
	private static String mapLibraryName(String nickname)
	{
		if (nickname == null)
		{
			throw new NullPointerException("nickname == null");
		}
		return "lib" + nickname + ".so";
	}
}
