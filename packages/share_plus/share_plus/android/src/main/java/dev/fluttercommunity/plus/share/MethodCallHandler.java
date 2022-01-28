// Copyright 2019 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package dev.fluttercommunity.plus.share;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface MethodInvoker {
  void invokeMethod(@NonNull String method, @Nullable Object arguments);
}


/** Handles the method calls for the plugin. */
class MethodCallHandler implements MethodChannel.MethodCallHandler, ShareCallback {

  private final Share share;
  private final Map<Integer, String> requests;
  private int requestCount = 5001;
  private final MethodInvoker invoker;

  MethodCallHandler(Share share, MethodInvoker invoker) {
    requests = new HashMap<>();
    this.invoker = invoker;
    this.share = share;
    this.share.setCallback(this);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onMethodCall(MethodCall call, @NonNull MethodChannel.Result result) {
    requestCount += 1;
    int requestCode = requestCount;
    switch (call.method) {
      case "share":
        expectMapArguments(call);
        // Android does not support showing the share sheet at a particular point on screen.
        requests.put(requestCode, "shareCallback");
        share.share(requestCode, (String) call.argument("text"), (String) call.argument("subject"));
        result.success(String.valueOf(requestCode));
        break;
      case "shareFiles":
        expectMapArguments(call);

        // Android does not support showing the share sheet at a particular point on screen.
        try {
          requests.put(requestCount, "shareFilesCallback");
          share.shareFiles(
                  requestCode,
              (List<String>) call.argument("paths"),
              (List<String>) call.argument("mimeTypes"),
              (String) call.argument("text"),
              (String) call.argument("subject"));
          result.success(String.valueOf(requestCode));
        } catch (IOException e) {
          result.error(e.getMessage(), null, null);
        }
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  private void expectMapArguments(MethodCall call) throws IllegalArgumentException {
    if (!(call.arguments instanceof Map)) {
      throw new IllegalArgumentException("Map argument expected");
    }
  }

  @Override
  public boolean onShared(int requestCode, boolean result) {
    if(requests.containsKey(requestCode)) {
      String method = requests.get(requestCode);
      if(method != null) {
        final Map<String, Object> args = new HashMap<>();
        args.put("requestCode", String.valueOf(requestCode));
        args.put("result", result);
        invoker.invokeMethod(method, args);
        requests.remove(requestCode);
        return true;
      }
    }
    return  false;
  }
}
