# CrowdNotifier-SDK for Android

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://github.com/CrowdNotifier/crowdnotifier-sdk-android/blob/master/LICENSE)
![Android Build](https://github.com/CrowdNotifier/crowdnotifier-sdk-android/workflows/Android%20Build/badge.svg)
![Android Tests](https://github.com/CrowdNotifier/crowdnotifier-sdk-android/workflows/Android%20Tests/badge.svg)

## CrowdNotifier
This repository implements a secure, decentralized, privacy-preserving presence tracing system. The proposal aims to simplify and accelerate the process of notifying individuals that shared a semi-public location with a SARS-CoV-2-positive person for a prolonged time without introducing new risks for users and locations. Existing proximity tracing systems (apps for contact tracing such as SwissCovid, Corona Warn App, and Immuni) notify only a subset of these people: those that were close enough for long enough. Current events have shown the need to notify all people that shared a space with a SARS-CoV-2-positive person. The proposed system aims to provide an alternative to increasing use of apps with similar intentions based on invasive collection or that are prone to abuse by authorities. The preliminary design aims to minimize privacy and security risks for individuals and communities, while guaranteeing the highest level of data protection and good usability and deployability.

The white paper this implementation is based on can be found here: [CrowdNotifier White Paper](https://github.com/CrowdNotifier/documents)

## Repositories
* Android SDK: [crowdnotifier-sdk-android](https://github.com/CrowdNotifier/crowdnotifier-sdk-android)
* iOS SDK: [crowdnotifier-sdk-ios](https://github.com/CrowdNotifier/crowdnotifier-sdk-ios)
* Android Demo App: [notifyme-app-android](https://github.com/notifyme-app/notifyme-app-android)
* iOS Demo App: [notifyme-app-ios](https://github.com/notifyme-app/notifyme-app-ios)
* Backend SDK: [notifyme-sdk-backend](https://github.com/notifyme-app/notifyme-sdk-backend)

## Work in Progress
The CrowdNotifierSDK for Android contains alpha-quality code only and is not yet complete. It has not yet been reviewed or audited for security and compatibility. We are both continuing the development and have started a security review. This project is truly open-source and we welcome any feedback on the code regarding both the implementation and security aspects.
This repository contains the open prototype SDK, so please focus your feedback for this repository on implementation issues.

## Further Documentation
The full set of documents for CrowdNotifier is at https://github.com/CrowdNotifier/documents. Please refer to the technical documents and whitepapers for a description of the implementation.

## Function overview

Name | Description | Function Name
---- | ----------- | -------------
getVenueInfo | Returns information about the data contained in a QR code, or null if the QR code does not have a valid format | `public static VenueInfo getVenueInfo(String qrCode, String expectedQrCodePrefix)`
addCheckin | Stores a check in given arrival time, departure time, notification key and the venue's public key. Returns the id of the stored entry. | `public static long addCheckIn(long arrivalTime, long departureTime, byte[] notificationKey, byte[] venuePublicKey,Context context)`
updateCheckin | Updates a checkin that has previously been stored | `public static boolean updateCheckIn(long id, long arrivalTime, long departureTime, byte[] notificationKey,byte[] venuePublicKey, Context context)`
checkForMatches | Given a set of published events with a known infected visitor, stores and returns those locally stored check ins that overlap with one of the problematic events | `public static List<ExposureEvent> checkForMatches(List<ProblematicEventInfo> publishedSKs, Context context)`
getExposureEvents | Returns all currently stored check ins that have previously matched a problematic event | `public static List<ExposureEvent> getExposureEvents(Context context)`
cleanUpOldData | Removes all check ins that are older than the specified number of days | `public static void cleanUpOldData(Context context, int maxDaysToKeep)`

## Installation

The SDK is available on JCenter and can be included directly as Gradle dependency:

```groovy
dependencies {
implementation 'org.crowdnotifier:crowdnotifier-sdk-android:0.1'
}
```

## Using the SDK

```java
// Get VenueInfo
VenueInfo venueInfo = CrowdNotifier.getVenueInfo(qrCode, prefix);

// Store a Check-In
long id = CrowdNotifier.addCheckIn(arrivalTime, departureTime, venueInfo.getNotificationKey(), venueInfo.getPublicKey(), getContext());

// Update a Check-In
CrowdNotifier.updateCheckIn(id, arrivalTime, departureTime, venueInfo.getNotificationKey(), venueInfo.getPublicKey(), getContext());

// Match published SKs against stored encrypted venue visits
List<ExposureEvent> newExposures = CrowdNotifier.checkForMatches(publishedSKs, getContext());

// Get all exposureEvents
List<ExposureEvent> allExposures = CrowdNotifier.getExposureEvents(getContext());

// Clean up old entries
CrowdNotifier.cleanUpOldData(getContext(), 10);
```

## License

This project is licensed under the terms of the MPL 2 license. See the [LICENSE](LICENSE) file.
