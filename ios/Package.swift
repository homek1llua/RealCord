// swift-tools-version:5.7
import PackageDescription

let package = Package(
    name: "RealCord",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "RealCord", targets: ["RealCord"])
    ],
    dependencies: [
        .package(url: "https://github.com/firebase/firebase-ios-sdk.git", from: "10.0.0"),
        .package(url: "https://github.com/onevcat/Kingfisher.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "RealCord",
            dependencies: [
                .product(name: "FirebaseAuth", package: "firebase-ios-sdk"),
                .product(name: "FirebaseFirestore", package: "firebase-ios-sdk"),
                .product(name: "FirebaseStorage", package: "firebase-ios-sdk"),
                .product(name: "FirebaseMessaging", package: "firebase-ios-sdk"),
                .product(name: "Kingfisher", package: "Kingfisher")
            ]
        )
    ]
)
