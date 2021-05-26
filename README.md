# CrowdNotifier-SDK for Android

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://github.com/CrowdNotifier/crowdnotifier-sdk-android/blob/master/LICENSE)
![Android Build](https://github.com/CrowdNotifier/crowdnotifier-sdk-android/workflows/Build/badge.svg)
![Android Tests](https://github.com/CrowdNotifier/crowdnotifier-sdk-android/workflows/Android%20Tests/badge.svg)

This repository contains a work-in-progress SDK for presence tracing based on the [CrowdNotifier protocol](https://github.com/CrowdNotifier/documents). The API and the underlying protocols are subject to change.

CrowdNotifier proposes a protocol for building secure, decentralized, privacy-preserving presence tracing systems. It simplifies and accelerates the process of notifying individuals that shared a semi-public location with a SARS-CoV-2-positive person for a prolonged time without introducing new risks for users and locations. Existing proximity tracing systems (apps for contact tracing such as SwissCovid, Corona Warn App, and Immuni) notify only a subset of these people: those that were close enough for long enough. Current events have shown the need to notify all people that shared a space with a SARS-CoV-2-positive person. The proposed system provides an alternative to other presence-tracing systems that are based on invasive collection or that are prone to abuse by authorities.

The CrowdNotifier design aims to minimize privacy and security risks for individuals and communities, while guaranteeing the highest level of data protection and good usability and deployability. For further details on the design, see the [CrowdNotifier White Paper](https://github.com/CrowdNotifier/documents).

### Work in Progress
The CrowdNotifier protocol is undergoing changes to improve its security and privacy properties. See [CrowdNotifier](https://github.com/CrowdNotifier/documents) for updates on the design. This SDK will be updated to reflect these changes.

The CrowdNotifierSDK for Android contains alpha-quality code only and is not yet complete. We are continuing the development of this library, and the API is likely to change. The library has not yet been reviewed or audited for security and compatibility.

## Repositories

This repository is part of a larger ecosystem. Please see the links below for SDKs for other platforms, and demo and backend applications that build on them.

* Android SDK: [crowdnotifier-sdk-android](https://github.com/CrowdNotifier/crowdnotifier-sdk-android)
* iOS SDK: [crowdnotifier-sdk-ios](https://github.com/CrowdNotifier/crowdnotifier-sdk-ios)
* TypeScript Reference Implementation: [crowdnotifier-ts](https://github.com/CrowdNotifier/crowdnotifier-ts)
* Android Demo App: [notifyme-app-android](https://github.com/notifyme-app/notifyme-app-android)
* iOS Demo App: [notifyme-app-ios](https://github.com/notifyme-app/notifyme-app-ios)
* Backend SDK: [notifyme-sdk-backend](https://github.com/notifyme-app/notifyme-sdk-backend)
* Web Apps: [notifyme-webpages](https://github.com/notifyme-app/notifyme-webpages)

You can find further information on the CrowdNotifier protocol in the [CrowdNotifier white paper](https://github.com/CrowdNotifier/documents)

## Installation

The SDK is available on Maven Central and can be included directly as Gradle dependency:

```groovy
dependencies {
  implementation 'org.crowdnotifier:crowdnotifier-sdk-android:4.0'
}
```

ATTENTION: Version 4.0 of the SDK is not backwards compatible with earlier versions!

## Using the SDK

```java
// Get VenueInfo
VenueInfo venueInfo = CrowdNotifier.getVenueInfo(qrCode, prefix);

// Store a Check-In
long id = CrowdNotifier.addCheckIn(arrivalTime, departureTime, venueInfo, getContext());

// Update a Check-In
CrowdNotifier.updateCheckIn(id, arrivalTime, departureTime, venueInfo, getContext());

// Delete a Check-In
CrowdNotifier.deleteCheckIn(id, getContext());

// Match published SKs against stored encrypted venue visits
List<ExposureEvent> newExposures = CrowdNotifier.checkForMatches(publishedSKs, getContext());

// Get all exposureEvents
List<ExposureEvent> allExposures = CrowdNotifier.getExposureEvents(getContext());

// Clean up old entries
CrowdNotifier.cleanUpOldData(getContext(), 10);

// Remove an Exposure Event
CrowdNotifier.removeExposure(getContext(), id);

// Generate an Entry QR Code String
String entryQrCode = CrowdNotifier.generateVenueInfo(...).toQrCodeString("https://example-base-url.org");

```

## Static methods of CrowdNotifier

The CrowdNotifier class implements the following static methods that can be used to interact with the system. The SDK only stores encrypted entries of check-ins as well as exposure matches. Any additional storage of data needs to
be handled by the app itself.

Name | Description | Function Name
---- | ----------- | -------------
getVenueInfo | Returns information about the data contained in a QR code, or null if the QR code does not have a valid format | `public static VenueInfo getVenueInfo(String qrCode, String expectedQrCodePrefix)`
addCheckin | Stores a check in given arrival time, departure time and a VenueInfo object. Returns the id of the stored entry. | `public static long addCheckIn(long arrivalTime, long departureTime, VenueInfo venueInfo, Context context)`
updateCheckin | Updates a checkin that has previously been stored | `public static boolean updateCheckIn(long id, long arrivalTime, long departureTime, VenueInfo venueInfo, Context context)`
deleteCheckin | Deletes a checkin that has previously been stored | `public static boolean deleteCheckIn(long id, Context context)`
checkForMatches | Given a set of published events with a known infected visitor, stores and returns those locally stored check ins that overlap with one of the problematic events | `public static List<ExposureEvent> checkForMatches(List<ProblematicEventInfo> publishedSKs, Context context)`
getExposureEvents | Returns all currently stored check ins that have previously matched a problematic event | `public static List<ExposureEvent> getExposureEvents(Context context)`
cleanUpOldData | Removes all check ins that are older than the specified number of days | `public static void cleanUpOldData(Context context, int maxDaysToKeep)`
removeExposure | Removes the exposure with the given ID | `public static void removeExposure(Context context, long exposureId)`
generateVenueInfo | Generates a VenueInfo with the given properties. | `public static VenueInfo generateVenueInfo(String description, String address, byte[] countryData, long validFrom, long validTo, byte[] masterPublicKey)`
generateUserUploadPayload | Generates an anonymised Venue Visit which can be used for Backward-Tracing in the Backend. | `public static UserUploadPayload generateUserUploadPayload(VenueInfo venueInfo, long startTimestamp, long endTimestamp)`

## Contributing

This project is truly open-source and we welcome any feedback on the code regarding both the implementation and security aspects. This repository contains the Android prototype SDK, so please focus your feedback for this repository on implementation issues.

Before proceeding, please read the [Code of Conduct](CODE_OF_CONDUCT.txt) to ensure positive and constructive interactions with the community.

## License

This project is licensed under the terms of the MPL 2 license. See the [LICENSE](LICENSE) file.
