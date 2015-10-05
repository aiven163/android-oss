package com.kickstarter.presenters;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.jakewharton.rxbinding.widget.RxTextView;
import com.kickstarter.KSApplication;
import com.kickstarter.R;
import com.kickstarter.libs.ApiErrorHandler;
import com.kickstarter.libs.CurrentUser;
import com.kickstarter.libs.Presenter;
import com.kickstarter.libs.RxUtils;
import com.kickstarter.libs.StringUtils;
import com.kickstarter.services.ApiClient;
import com.kickstarter.services.ApiError;
import com.kickstarter.services.apiresponses.AccessTokenEnvelope;
import com.kickstarter.ui.activities.LoginActivity;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

public class LoginPresenter extends Presenter<LoginActivity> {
  @Inject ApiClient client;
  @Inject CurrentUser currentUser;
  private final PublishSubject<Void> loginClick = PublishSubject.create();
  private boolean forward = false;

  @Override
  protected void onCreate(@NonNull final Context context, @Nullable Bundle savedInstanceState) {
    super.onCreate(context, savedInstanceState);
    ((KSApplication) context.getApplicationContext()).component().inject(this);

    final Observable<CharSequence> email = viewSubject
      .flatMap(v -> RxTextView.textChanges(v.emailEditText));

    final Observable<CharSequence> password = viewSubject
      .flatMap(v -> RxTextView.textChanges(v.passwordEditText));

    final Observable<Pair<String, String>> emailAndPassword =
      RxUtils.combineLatestPair(email, password)
      .map(ep -> Pair.create(ep.first.toString(), ep.second.toString()));

    final Observable<Boolean> isValid = emailAndPassword
      .map(ep -> LoginPresenter.isValid(ep.first, ep.second));

    addSubscription(RxUtils.combineLatestPair(viewSubject, isValid)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(viewAndValid -> viewAndValid.first.setFormEnabled(viewAndValid.second))
    );

    addSubscription(RxUtils.takeWhen(emailAndPassword, loginClick)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(ep -> submit(ep.first, ep.second))
    );
  }

  public void takeForward(final boolean forward) {
    this.forward = forward;
  }

  private static boolean isValid(@NonNull final String email, @NonNull final String password) {
    return StringUtils.isEmail(email) && password.length() > 0;
  }

  public void takeLoginClick() {
    loginClick.onNext(null);
  }

  private void submit(@NonNull final String email, @NonNull final String password) {
    client.login(email, password)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::success, this::error);
  }

  private void success(@NonNull final AccessTokenEnvelope envelope) {
    currentUser.login(envelope.user(), envelope.accessToken());

    if (hasView()) {
      view().onSuccess(forward);
    }
  }

  private void error(@NonNull final Throwable e) {
    if (!hasView()) {
      return;
    }

    new ApiErrorHandler(e, view()) {
      @Override
      public void handleApiError(@NonNull final ApiError apiError) {
        switch (apiError.errorEnvelope().ksrCode()) {
          case TFA_REQUIRED:
          case TFA_FAILED:
            view().startTwoFactorActivity(forward);
            break;
          case INVALID_XAUTH_LOGIN:
            displayError(R.string.Login_does_not_match_any_of_our_records);
            break;
          default:
            displayError(R.string.Unable_to_login);
            break;
        }

      }
    }.handleError();
  }
}
