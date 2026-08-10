// swift-tools-version: 5.9

import PackageDescription

let dingVersion = "0.5.1"
let dingChecksum = "d5230e37d7ba8caaf62706f01f91bbd2d1016a3422088b765586ae06bc1b9d84"

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
            url: "https://github.com/easyhooon/ding/releases/download/\(dingVersion)/Ding.xcframework.zip",
            checksum: dingChecksum
        ),
    ]
)
