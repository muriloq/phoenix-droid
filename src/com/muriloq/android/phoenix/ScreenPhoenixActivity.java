package com.muriloq.android.phoenix;

import android.os.Bundle;

import com.muriloq.android.phoenix.controller.ScreenController;

public class ScreenPhoenixActivity extends PhoenixActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    view.setController(new ScreenController(this));

    // Enable view key events
    view.setFocusable(true);
    view.setFocusableInTouchMode(true);

  }
}