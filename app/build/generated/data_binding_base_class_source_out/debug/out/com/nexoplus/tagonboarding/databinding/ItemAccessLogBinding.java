// Generated by view binder compiler. Do not edit!
package com.nexoplus.tagonboarding.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.google.android.material.card.MaterialCardView;
import com.nexoplus.tagonboarding.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ItemAccessLogBinding implements ViewBinding {
  @NonNull
  private final MaterialCardView rootView;

  @NonNull
  public final TextView accessTimeText;

  @NonNull
  public final TextView deviceText;

  @NonNull
  public final TextView laneText;

  @NonNull
  public final TextView statusText;

  @NonNull
  public final TextView tagIdText;

  @NonNull
  public final TextView userNameText;

  @NonNull
  public final TextView vehicleNumberText;

  private ItemAccessLogBinding(@NonNull MaterialCardView rootView, @NonNull TextView accessTimeText,
      @NonNull TextView deviceText, @NonNull TextView laneText, @NonNull TextView statusText,
      @NonNull TextView tagIdText, @NonNull TextView userNameText,
      @NonNull TextView vehicleNumberText) {
    this.rootView = rootView;
    this.accessTimeText = accessTimeText;
    this.deviceText = deviceText;
    this.laneText = laneText;
    this.statusText = statusText;
    this.tagIdText = tagIdText;
    this.userNameText = userNameText;
    this.vehicleNumberText = vehicleNumberText;
  }

  @Override
  @NonNull
  public MaterialCardView getRoot() {
    return rootView;
  }

  @NonNull
  public static ItemAccessLogBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ItemAccessLogBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.item_access_log, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ItemAccessLogBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.accessTimeText;
      TextView accessTimeText = ViewBindings.findChildViewById(rootView, id);
      if (accessTimeText == null) {
        break missingId;
      }

      id = R.id.deviceText;
      TextView deviceText = ViewBindings.findChildViewById(rootView, id);
      if (deviceText == null) {
        break missingId;
      }

      id = R.id.laneText;
      TextView laneText = ViewBindings.findChildViewById(rootView, id);
      if (laneText == null) {
        break missingId;
      }

      id = R.id.statusText;
      TextView statusText = ViewBindings.findChildViewById(rootView, id);
      if (statusText == null) {
        break missingId;
      }

      id = R.id.tagIdText;
      TextView tagIdText = ViewBindings.findChildViewById(rootView, id);
      if (tagIdText == null) {
        break missingId;
      }

      id = R.id.userNameText;
      TextView userNameText = ViewBindings.findChildViewById(rootView, id);
      if (userNameText == null) {
        break missingId;
      }

      id = R.id.vehicleNumberText;
      TextView vehicleNumberText = ViewBindings.findChildViewById(rootView, id);
      if (vehicleNumberText == null) {
        break missingId;
      }

      return new ItemAccessLogBinding((MaterialCardView) rootView, accessTimeText, deviceText,
          laneText, statusText, tagIdText, userNameText, vehicleNumberText);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
