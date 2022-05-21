# Forked Mastodon for Android

This is the repository for an officially forked Android app for Mastodon.

Learn more about the official app in the [blog post](https://blog.joinmastodon.org/2022/02/official-mastodon-for-android-app-is-coming-soon/).

## Changes

* [Enable "Unlisted" as a visibility option](https://github.com/sk22/mastodon-android-fork/tree/feature/enable-unlisted)
  ([Pull request](https://github.com/mastodon/mastodon-android/pull/103)) and
  [set as default](https://github.com/sk22/mastodon-android-fork/tree/feature/enable-unlisted-as-default)
* [Add "Federation" tab and change Discover tab order](https://github.com/sk22/mastodon-android-fork/tree/feature/add-federated-timeline) ([Fixes issue](https://github.com/mastodon/mastodon-android/issues/8))
* [Add image description button and viewer](https://github.com/sk22/mastodon-android-fork/tree/feature/display-alt-text) ([Pull request](https://github.com/mastodon/mastodon-android/pull/129))
* [Implement pinning posts and displaying pinned posts](https://github.com/sk22/mastodon-android-fork/tree/feature/pin-posts) ([Pull request](https://github.com/mastodon/mastodon-android/pull/140))
* [Display full image when adding image description](https://github.com/sk22/mastodon-android-fork/tree/feature/compose-image-description-full-image) ([Pull request](https://github.com/mastodon/mastodon-android/pull/182))
* [Always preserve content warnings when replying](https://github.com/sk22/mastodon-android-fork/tree/feature/always-preserve-cw) ([Fixes issue](https://github.com/mastodon/mastodon-android/issues/113))
* [Make back button return to the home tab before exiting the app](https://github.com/sk22/mastodon-android-fork/tree/feature/back-returns-home) ([Fixes issue](https://github.com/mastodon/mastodon-android/issues/118))

## Building

As this app is using Java 17 features, you need JDK 17 or newer to build it. Other than that, everything is pretty standard. You can either import the project into Android Studio and build it from there, or run the following command in the project directory:

```
./gradlew assembleRelease
```

## License

This project is released under the [GPL-3 License](./LICENSE).
