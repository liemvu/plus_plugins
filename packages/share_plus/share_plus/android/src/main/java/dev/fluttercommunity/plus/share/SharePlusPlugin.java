// Copyright 2019 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package dev.fluttercommunity.plus.share;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodChannel;

/** Plugin method host for presenting a share sheet via Intent */
public class SharePlusPlugin implements FlutterPlugin, ActivityAware, MethodInvoker {

  private static final String CHANNEL = "dev.fluttercommunity.plus/share";
  private Share share;
  private MethodChannel methodChannel;
  private ActivityPluginBinding binding;

  @Override
  public void onAttachedToEngine(FlutterPluginBinding binding) {
    methodChannel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL);
    share = new Share(binding.getApplicationContext(), null);
    MethodCallHandler handler = new MethodCallHandler(share, this);
    methodChannel.setMethodCallHandler(handler);

  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    methodChannel.setMethodCallHandler(null);
    methodChannel = null;
    share = null;
  }

  @Override
  public void onAttachedToActivity(ActivityPluginBinding binding) {
    share.setActivity(binding.getActivity());
    binding.addActivityResultListener(share);
    this.binding = binding;
  }

  @Override
  public void onDetachedFromActivity() {
    share.setActivity(null);
    binding.removeActivityResultListener(share);
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity();
  }


  @Override
  public void invokeMethod(@NonNull String method, @Nullable Object arguments) {
    if(methodChannel != null) {
      methodChannel.invokeMethod(method, arguments);
    }
  }
}
