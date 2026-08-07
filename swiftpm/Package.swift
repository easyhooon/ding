// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "Ding",
    platforms: [
        .iOS(.v13),
    ],
    products: [
        .library(
            name: "Ding",
            targets: ["Ding"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "Ding",
            path: "Artifacts/Ding.xcframework"
        ),
    ]
)
