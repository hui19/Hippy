package com.tencent.mtt.hippy.example.module;

import android.content.Intent;
import android.util.Log;

import com.tencent.mtt.hippy.HippyEngineContext;
import com.tencent.mtt.hippy.HippyRootView;
import com.tencent.mtt.hippy.annotation.HippyMethod;
import com.tencent.mtt.hippy.annotation.HippyNativeModule;
import com.tencent.mtt.hippy.common.HippyMap;
import com.tencent.mtt.hippy.example.BaseActivity;
import com.tencent.mtt.hippy.modules.Promise;
import com.tencent.mtt.hippy.modules.nativemodules.HippyNativeModuleBase;

/**
 * @Description: the extend module
 * @author: edsheng
 * @date: 2018/3/22 10:51
 * @version: V1.0
 * 展示自定义module
 */

@HippyNativeModule(name = TestModule.CLASSNAME, names = {"TestModuleA", "TestModuleB"})
public class TestModule extends HippyNativeModuleBase {
    final static String CLASSNAME = "TestModule";

    public TestModule(HippyEngineContext context) {
        super(context);
    }

    @HippyMethod(name = "debug")
    public void debug(int instanceid) {
        HippyRootView hippyRootView = mContext.getInstance(instanceid);
        Intent intent = new Intent();
        intent.setClass(hippyRootView.getContext(), BaseActivity.class);
        hippyRootView.getContext().startActivity(intent);
    }


    /***
     * TestModule
     * @param log
     * 自定义了扩展了一个log的接口并且无回调
     */
    @HippyMethod(name = "log")
    public void log(String log) {
        //这里回来的参数可以为java的基础类型，和hippymap与hippyarry,但是前端调用的时候必须对应上
        Log.d("TestModule", log);
    }

    /**
     * 展示前端回来的是一个对象
     *
     * @param hippyMap
     */
    @HippyMethod(name = "helloNative")
    public void helloNative(HippyMap hippyMap) {
        //这里回来的参数可以为java的基础类型，和hippymap与hippyarry,但是前端调用的时候必须对应上
        String hello = hippyMap.getString("hello");
        Log.d("TestModule", hello);
    }

    /**
     * 展示终端需要给前端回调参数
     *
     * @param hippyMap
     */
    @HippyMethod(name = "helloNativeWithPromise")
    public void helloNativeWithPromise(HippyMap hippyMap, Promise promise) {
        HippyMap back = new HippyMap();
        back.pushString("body", "CgwQASkADwoGFTFfMTMxODQzOTQyMTg0MTMxMDAxOBABIAE9AAECMQkAAwZnaHR0cDovL3FxcHVibGljLnFwaWMuY24vcXFfcHVibGljX2NvdmVyLzAvMC0yMzg4MDcwMDM3LTI1QUZBQjVGMEEyNjVEMDUwMEVGNzM3NjU0QTQ3Mjk1X3BhYXNfMTA4XzgwLzMyMAZnaHR0cDovL3FxcHVibGljLnFwaWMuY24vcXFfcHVibGljX2NvdmVyLzAvMC0yNTc1OTc2NDEwLTQxMTc5Rjc0MzFCMUMwQjhDMkY2MkUzMTA1Mjg4REZFX3BhYXNfMTA4XzgwLzMyMAZnaHR0cDovL3FxcHVibGljLnFwaWMuY24vcXFfcHVibGljX2NvdmVyLzAvMC0yMzY1MDYwMDU1LUJEQzFENEE2MkUxM0M5MkZBQ0MzRUY3MzNFMDI0NENCX3BhYXNfMTA4XzgwLzMyMCoWACYANgBGAAs5AAEGFFsv5aSnVl3oiJznvZEgIDcw6K+ESgYAEAEsPEYAXGoMHAsLWgYAFgAmADYAC2oGABYAJgA2AAt2AID/mQyqBgAWACYANgBGAFYAZgB2AIYAC7oGABYALAvKCAwL2gYAFgAsPAvmAPoPBgAQAQv6EAYAFgAmADYATFYAYAF2AIwL+hEMHCw8RgBWAAv6EgYAFgAmADwL9hMA/BT6FQYAFgAsPAv6FgYAFgAmADYARgBWAGYAdgCJDJYAoP+2AMYAC/oXBgAcLDYARgBWAGYAcAGAAZABoAG8zAv6GAYAFgAmADYAC0ZZODbjgIropb/muLjorrDjgIvmvJTlkZjlq4zmnI3oo4XlpKrmmrTpnLLnvaLmvJTvvIzmnajmtIHmib7mnaXigJzoo7jmm7/igJ3vvIzmsqHmnInnqb/luK5XAAAD/XFiOi8vZXh0L3JlYWQ/Y2lkPU10dFRhZ1NvdXJjZSZ0eXBlPTMmbXR0c3VtbWFyeWlkPTEzMTg0M");
        back.pushInt("code", 0);
        back.pushString("className", "MTT.GetHomepageFeedsTabListsRsp");

        HippyMap inner = new HippyMap();
        inner.pushInt("QQ-S-Encrypt", 17);
        inner.pushString("Cache-Control", "no-cache");
        inner.pushString("OkHttp-Selected-Protocol", "http/1.1");
        inner.pushString("Content-Type", "application/multipart-formdata");
        inner.pushString("Date", "Tue, 16 Mar 2021 10:53:08 GMT");
        inner.pushString("Server", "QBServer");
        inner.pushString("QQ-S-ZIP", "gzip");
        inner.pushInt("Content-Length", 26448);
        inner.pushString("OkHttp-Response-Source", "NETWORK 200");
        inner.pushString("Connection", "keep-alive");

        back.pushMap("headers", inner);
        promise.resolve(back);

    }
}
