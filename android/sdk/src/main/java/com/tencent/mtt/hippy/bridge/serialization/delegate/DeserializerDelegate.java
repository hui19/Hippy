package com.tencent.mtt.hippy.bridge.serialization.delegate;

import com.tencent.mtt.hippy.runtime.builtins.JSSharedArrayBuffer;
import com.tencent.mtt.hippy.runtime.builtins.wasm.WasmModule;
import com.tencent.mtt.hippy.serialization.exception.DataCloneDeserializationException;
import com.tencent.mtt.hippy.serialization.recommend.Deserializer;

@SuppressWarnings("JavaJniMissingFunction")
public class DeserializerDelegate implements Deserializer.Delegate {
  private static final DeserializerDelegate instance = new DeserializerDelegate();

  public static DeserializerDelegate getInstance() {
    return instance;
  }

  private DeserializerDelegate() {

  }

  @Override
  public Object readHostObject(Deserializer deserializer) {
    throw new DataCloneDeserializationException();
  }

  @Override
  public JSSharedArrayBuffer getSharedArrayBufferFromId(Deserializer deserializer, int clone_id) {
    return getSharedArrayBufferFromId(clone_id);
  }

  @Override
  public WasmModule getWasmModuleFromId(Deserializer deserializer, int transfer_id) {
    return getWasmModuleFromId(transfer_id);
  }

  public static native JSSharedArrayBuffer getSharedArrayBufferFromId(int clone_id);
  public static native WasmModule getWasmModuleFromId(int transfer_id);
}
