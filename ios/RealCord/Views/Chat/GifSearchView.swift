import SwiftUI
import Kingfisher

struct GifSearchView: View {
    @StateObject private var gifService = GifService()
    @State private var searchQuery = ""
    @State private var results: [GifResult] = []
    @State private var isLoading = false
    let onSelect: (String) -> Void

    private let columns = [
        GridItem(.flexible()),
        GridItem(.flexible())
    ]

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(Color(hex: "#80848E"))
                TextField("Search GIFs", text: $searchQuery)
                    .foregroundColor(.white)
                    .onSubmit(search)
            }
            .padding(12)
            .background(Color(hex: "#1E1F22"))
            .cornerRadius(8)
            .padding()

            if isLoading {
                Spacer()
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                Spacer()
            } else if results.isEmpty {
                Spacer()
                VStack(spacing: 8) {
                    Image(systemName: "photo.on.rectangle")
                        .font(.system(size: 40))
                        .foregroundColor(Color(hex: "#80848E"))
                    Text("Search for GIFs")
                        .foregroundColor(Color(hex: "#80848E"))
                }
                Spacer()
            } else {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 8) {
                        ForEach(results) { gif in
                            if let url = gif.previewUrl, let fullUrl = gif.fullUrl {
                                KFImage(URL(string: url))
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(height: 120)
                                    .clipped()
                                    .cornerRadius(8)
                                    .onTapGesture {
                                        onSelect(fullUrl)
                                    }
                            }
                        }
                    }
                    .padding(.horizontal)
                }
            }
        }
        .background(Color(hex: "#313338"))
        .onAppear {
            loadTrending()
        }
    }

    private func search() {
        guard !searchQuery.isEmpty else {
            loadTrending()
            return
        }
        isLoading = true
        Task {
            do {
                let results = try await gifService.search(query: searchQuery)
                await MainActor.run {
                    self.results = results
                    self.isLoading = false
                }
            } catch {
                await MainActor.run { self.isLoading = false }
            }
        }
    }

    private func loadTrending() {
        isLoading = true
        Task {
            do {
                let results = try await gifService.trending()
                await MainActor.run {
                    self.results = results
                    self.isLoading = false
                }
            } catch {
                await MainActor.run { self.isLoading = false }
            }
        }
    }
}
