package xyz.kuailemao.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * RAG 知识库自动加载器
 * 功能：
 * 1. 加载 classpath:knowledge/ 下所有文件（md/pdf/txt/docx）
 * 2. 自动识别新文件 → 增量插入
 * 3. 已存在的文件 → 自动跳过
 * 4. 无重复、无冗余、最干净的向量库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorDataInitializer implements CommandLineRunner {

    private final VectorStore vectorStore;
    private final TokenTextSplitter tokenTextSplitter;

    // 知识库文件夹
    private static final String KNOWLEDGE_PATH = "classpath:knowledge/*.*";

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 开始检查并更新知识库...");

        // 1. 获取向量库中【已经存在的文件名】
        Set<String> existedFiles = getExistedFileNames();
        log.info("✅ 向量库中已存在文件数量：{}", existedFiles.size());

        // 2. 读取本地文件夹所有文件
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources(KNOWLEDGE_PATH);

        List<Document> finalDocuments = new ArrayList<>();

        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            log.info("📄 检查文件：{}", fileName);

            // 3. 已存在 → 跳过
            if (existedFiles.contains(fileName)) {
                log.info("➡️  文件已存在，跳过：{}", fileName);
                continue;
            }

            // 4. 不存在 → 读取并加入向量库
            log.info("🆕 发现新文件，开始加载：{}", fileName);
            List<Document> documents = loadFile(resource);

            // 5. 把文件名存入元数据（用于下次去重）
            documents.forEach(doc -> {
                doc.getMetadata().put("fileName", fileName);
            });

            finalDocuments.addAll(documents);
        }

        // 6. 文本分块
        if (!finalDocuments.isEmpty()) {
            List<Document> splitDocs = tokenTextSplitter.split(finalDocuments);
            vectorStore.add(splitDocs);
            log.info("✅ 本次新增向量完成！共加载 {} 条数据", splitDocs.size());
        } else {
            log.info("✅ 没有新文件需要加载，知识库已是最新！");
        }
    }

    // =============================================
    // 从向量库查询所有已存在的 fileName（去重用）
    // =============================================
    private Set<String> getExistedFileNames() {
        Set<String> fileNames = new HashSet<>();
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("init")
                            .topK(1000)
                            .build()
            );

            for (Document doc : docs) {
                String name = (String) doc.getMetadata().get("fileName");
                if (name != null) {
                    fileNames.add(name);
                }
            }
        } catch (Exception e) {
            log.warn("向量库为空，首次加载...");
        }
        return fileNames;
    }


    // 读取文件（支持 MD / TXT / PDF）
    private List<Document> loadFile(Resource resource) throws Exception {
        String fileName = resource.getFilename();

        if (fileName.endsWith(".pdf")) {
            return new PagePdfDocumentReader(resource).read();
        } else {

            return new TextReader(resource).read();
        }
    }
}