import SwiftUI

struct ContentView: View {
    @State private var title = "公众号文章标题"
    @State private var bodyText = """
    在这里编写公众号文章内容。

    后续可将 Android 端的编辑、模板和预览能力迁移到 iOS。
    """

    var body: some View {
        NavigationStack {
            Form {
                Section("文章信息") {
                    TextField("标题", text: $title)
                }

                Section("正文") {
                    TextEditor(text: $bodyText)
                        .frame(minHeight: 260)
                        .font(.body)
                }

                Section("预览") {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(title)
                            .font(.title2.weight(.semibold))
                        Text(bodyText)
                            .font(.body)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 6)
                }
            }
            .navigationTitle("公众号编辑器")
        }
    }
}

#Preview {
    ContentView()
}
