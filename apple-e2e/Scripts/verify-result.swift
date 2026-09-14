import Foundation
import CoreFoundation

enum VerificationError: Error, CustomStringConvertible {
    case invalidArguments
    case invalidJSONObject(String)
    case missingValue(String)
    case mismatch(path: String, expected: Any, actual: Any)

    var description: String {
        switch self {
        case .invalidArguments:
            return "Usage: verify-result.swift <payload.json> <result.json>"
        case let .invalidJSONObject(path):
            return "Expected a JSON object at \(path)"
        case let .missingValue(path):
            return "Missing JSON value at \(path)"
        case let .mismatch(path, expected, actual):
            return "Expected \(path) to be \(expected), but was \(actual)"
        }
    }
}

func loadJSONObject(at path: String) throws -> [String: Any] {
    let data = try Data(contentsOf: URL(fileURLWithPath: path))
    guard let object = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
        throw VerificationError.invalidJSONObject(path)
    }
    return object
}

func value(at keyPath: String, in root: [String: Any]) throws -> Any {
    var value: Any = root
    for key in keyPath.split(separator: ".").map(String.init) {
        guard let dictionary = value as? [String: Any], let next = dictionary[key] else {
            throw VerificationError.missingValue(keyPath)
        }
        value = next
    }
    return value
}

func assertValue(_ expected: Any, at keyPath: String, in result: [String: Any]) throws {
    let actual = try value(at: keyPath, in: result)
    guard let expectedObject = expected as? NSObject,
          let actualObject = actual as? NSObject else {
        throw VerificationError.mismatch(path: keyPath, expected: expected, actual: actual)
    }

    let expectedIsBoolean = CFGetTypeID(expectedObject) == CFBooleanGetTypeID()
    let actualIsBoolean = CFGetTypeID(actualObject) == CFBooleanGetTypeID()
    guard expectedIsBoolean == actualIsBoolean, expectedObject.isEqual(actualObject) else {
        throw VerificationError.mismatch(path: keyPath, expected: expected, actual: actual)
    }
}

func assertMatchesPayload(
    payloadKeyPath: String,
    resultKeyPath: String,
    payload: [String: Any],
    result: [String: Any]
) throws {
    try assertValue(
        value(at: payloadKeyPath, in: payload),
        at: resultKeyPath,
        in: result
    )
}

do {
    guard CommandLine.arguments.count == 3 else {
        throw VerificationError.invalidArguments
    }

    let payload = try loadJSONObject(at: CommandLine.arguments[1])
    let result = try loadJSONObject(at: CommandLine.arguments[2])

    for (keyPath, expected) in [
        ("type", "remote-notification"),
        ("source", "apns"),
        ("tag", "apns"),
        ("platform", "ios"),
        ("transport", "apns"),
        ("capturePoint", "foreground"),
    ] {
        try assertValue(expected, at: keyPath, in: result)
    }

    for (payloadKeyPath, resultKeyPath) in [
        ("aps.alert.title", "title"),
        ("aps.alert.body", "body"),
        ("aps.alert.title", "notification.title"),
        ("aps.alert.body", "notification.body"),
        ("aps.alert.title", "rawDeliveredPayload.aps.alert.title"),
        ("aps.alert.body", "rawDeliveredPayload.aps.alert.body"),
        ("ding-e2e-id", "data.ding-e2e-id"),
        ("ding-e2e-id", "rawDeliveredPayload.ding-e2e-id"),
        ("nested.count", "data.nested.count"),
        ("nested.enabled", "data.nested.enabled"),
        ("nested.count", "rawDeliveredPayload.nested.count"),
        ("nested.enabled", "rawDeliveredPayload.nested.enabled"),
    ] {
        try assertMatchesPayload(
            payloadKeyPath: payloadKeyPath,
            resultKeyPath: resultKeyPath,
            payload: payload,
            result: result
        )
    }

    print("Ding result JSON matches the simulated APNs payload.")
} catch {
    FileHandle.standardError.write(Data("\(error)\n".utf8))
    exit(1)
}
