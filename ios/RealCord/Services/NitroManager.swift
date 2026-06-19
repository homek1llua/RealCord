import Foundation

class NitroManager {
    static let shared = NitroManager()
    private let godUser = "it"

    func parseDuration(_ raw: String) -> TimeInterval? {
        let trimmed = raw.trimmingCharacters(in: .whitespaces)
        if trimmed.hasSuffix("d") {
            guard let val = Double(trimmed.dropLast()) else { return nil }
            return val * 86400
        } else if trimmed.hasSuffix("h") {
            guard let val = Double(trimmed.dropLast()) else { return nil }
            return val * 3600
        } else if trimmed.hasSuffix("m") {
            guard let val = Double(trimmed.dropLast()) else { return nil }
            return val * 60
        }
        return nil
    }

    func parseTier(_ raw: String) -> String {
        let lower = raw.lowercased()
        if lower.contains("premium") { return "premium" }
        return "normal"
    }

    struct NitroCommand {
        let targetUid: String?
        let duration: TimeInterval
        let tier: String
    }

    func parseNitroCommand(_ content: String, senderUid: String, senderName: String) -> NitroCommand? {
        let parts = content.split(separator: " ").map(String.init)
        guard parts.count >= 2 else { return nil }

        let isSelf = parts[0].lowercased() == "/nitros"
        let isOther = parts[0].lowercased() == "/nitro"

        guard isSelf || isOther else { return nil }

        if isOther && parts.count < 3 { return nil }

        if isSelf {
            guard let duration = parseDuration(parts[1]) else { return nil }
            let tier = parts.count >= 3 ? parseTier(parts[2]) : "normal"
            return NitroCommand(targetUid: senderUid, duration: duration, tier: tier)
        } else {
            let mention = parts[1]
            guard let duration = parseDuration(parts[2]) else { return nil }
            let tier = parts.count >= 4 ? parseTier(parts[3]) : "normal"
            return NitroCommand(targetUid: mention, duration: duration, tier: tier)
        }
    }

    func calculateExpiry(from duration: TimeInterval) -> Date {
        Date().addingTimeInterval(duration)
    }

    func isGodUser(username: String) -> Bool {
        username.lowercased() == godUser
    }
}
