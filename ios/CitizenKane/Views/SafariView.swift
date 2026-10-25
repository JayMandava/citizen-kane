import SafariServices
import SwiftUI

/// The in-app browser sheet used for "read the full article" — keeps the
/// reader inside the app's chrome instead of fully backgrounding the
/// app the way an external `Link` to Safari would.
struct SafariView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        SFSafariViewController(url: url)
    }

    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {}
}
