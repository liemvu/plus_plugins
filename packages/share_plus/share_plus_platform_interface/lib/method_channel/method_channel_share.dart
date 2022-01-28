// Copyright 2019 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

import 'dart:async';
import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:meta/meta.dart' show visibleForTesting;
import 'package:mime/mime.dart' show lookupMimeType;

import 'package:share_plus_platform_interface/share_plus_platform_interface.dart';

/// Plugin for summoning a platform share sheet.
class MethodChannelShare extends SharePlatform {
  final _requests = <String, Completer>{};

  /// [MethodChannel] used to communicate with the platform side.
  @visibleForTesting
  static const MethodChannel channel =
      MethodChannel('dev.fluttercommunity.plus/share');

  MethodChannelShare() {
    channel.setMethodCallHandler(methodCallHandler);
  }

  Future<dynamic> methodCallHandler(MethodCall? call) async {
    if (call?.arguments != null &&
        (call?.method == "shareCallback" ||
            call?.method == "shareFilesCallback")) {
      final args = call!.arguments as Map;
      final code = args["requestCode"];
      final completer = _requests[code];

      if (completer != null) {
        completer.complete(args["result"]);
      }
    }
  }

  /// Summons the platform's share sheet to share text.
  @override
  Future<dynamic> share(
    String text, {
    String? subject,
    Rect? sharePositionOrigin,
    Duration timeout = const Duration(milliseconds: 60000),
  }) async {
    assert(text.isNotEmpty);
    final params = <String, dynamic>{
      'text': text,
      'subject': subject,
    };

    if (sharePositionOrigin != null) {
      params['originX'] = sharePositionOrigin.left;
      params['originY'] = sharePositionOrigin.top;
      params['originWidth'] = sharePositionOrigin.width;
      params['originHeight'] = sharePositionOrigin.height;
    }
    final code = await channel.invokeMethod('share', params);
    if (code == null || !(code is String)) {
      return false;
    }

    final completer = Completer();
    _requests[code] = completer;

    return Future.any([
      completer.future,
      Future.delayed(timeout).then((_) {
        _requests.remove(code);
        return false;
      })
    ]);
  }

  /// Summons the platform's share sheet to share multiple files.
  @override
  Future<dynamic> shareFiles(
    List<String> paths, {
    List<String>? mimeTypes,
    String? subject,
    String? text,
    Rect? sharePositionOrigin,
    Duration timeout = const Duration(milliseconds: 60000),
  }) async {
    assert(paths.isNotEmpty);
    assert(paths.every((element) => element.isNotEmpty));
    final params = <String, dynamic>{
      'paths': paths,
      'mimeTypes': mimeTypes ??
          paths.map((String path) => _mimeTypeForPath(path)).toList(),
    };

    if (subject != null) params['subject'] = subject;
    if (text != null) params['text'] = text;

    if (sharePositionOrigin != null) {
      params['originX'] = sharePositionOrigin.left;
      params['originY'] = sharePositionOrigin.top;
      params['originWidth'] = sharePositionOrigin.width;
      params['originHeight'] = sharePositionOrigin.height;
    }

    final code = await channel.invokeMethod('shareFiles', params);
    if (code == null || !(code is String)) {
      return false;
    }

    final completer = Completer<bool>();
    _requests[code] = completer;
    return Future.any([
      completer.future,
      Future.delayed(timeout).then((value) {
        _requests.remove(code);
        return false;
      })
    ]);
  }

  static String _mimeTypeForPath(String path) {
    return lookupMimeType(path) ?? 'application/octet-stream';
  }
}
