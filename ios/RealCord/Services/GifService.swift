import Foundation

struct GifResult: Codable, Identifiable {
    let id: String
    let title: String
    let media: [GifMedia]?

    var previewUrl: String? {
        media?.first?.tinygif?.url
    }

    var fullUrl: String? {
        media?.first?.gif?.url
    }

    enum CodingKeys: String, CodingKey {
        case id, title, media
    }
}

struct GifMedia: Codable {
    let gif: GifMediaInfo?
    let tinygif: GifMediaInfo?
}

struct GifMediaInfo: Codable {
    let url: String
    let dims: [Int]?
    let size: Int?
}

struct GifResponse: Codable {
    let results: [GifResult]?
    let next: String?
}

class GifService: ObservableObject {
    static let shared = GifService()
    private let apiKey = "LIVDSRZULELA"
    private let baseURL = "https://g.tenor.com/v1"

    func search(query: String, limit: Int = 30) async throws -> [GifResult] {
        let encoded = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? query
        let urlString = "\(baseURL)/search?q=\(encoded)&key=\(apiKey)&limit=\(limit)&media_filter=minimal"
        guard let url = URL(string: urlString) else { return [] }

        let (data, _) = try await URLSession.shared.data(from: url)
        let response = try JSONDecoder().decode(GifResponse.self, from: data)
        return response.results ?? []
    }

    func trending(limit: Int = 30) async throws -> [GifResult] {
        let urlString = "\(baseURL)/trending?key=\(apiKey)&limit=\(limit)&media_filter=minimal"
        guard let url = URL(string: urlString) else { return [] }

        let (data, _) = try await URLSession.shared.data(from: url)
        let response = try JSONDecoder().decode(GifResponse.self, from: data)
        return response.results ?? []
    }
}
