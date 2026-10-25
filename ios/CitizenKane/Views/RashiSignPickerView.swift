import SwiftUI

/// A dedicated picker rather than a plain `Picker`, so the Telugu name and
/// its English gloss can sit on their own lines instead of being crammed
/// into one row — mixing scripts inline in a single `Text` reads cramped at
/// Form-row width.
struct RashiSignPickerView: View {
    @Binding var selection: RashiSign
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        List(RashiSign.allCases) { sign in
            Button {
                selection = sign
                dismiss()
            } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(sign.teluguName)
                            .font(.body)
                            .foregroundStyle(.primary)
                        Text(sign.englishName)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    if sign == selection {
                        Image(systemName: "checkmark")
                            .foregroundStyle(.tint)
                    }
                }
            }
        }
        .navigationTitle("Your Sign")
    }
}
