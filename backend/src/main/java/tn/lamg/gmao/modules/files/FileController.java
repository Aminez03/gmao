package tn.lamg.gmao.modules.files;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.lamg.gmao.common.*;
import tn.lamg.gmao.modules.patrimoine.model.*;
import tn.lamg.gmao.security.*;

@RestController
@RequestMapping("/api/v1")
public class FileController {

    final EntityStore store;
    final CurrentUser current;
    final AccessPolicy policy;
    final DtoMapper mapper;
    final JdbcTemplate jdbc;
    final String origin;

    public FileController(
        EntityStore s,
        CurrentUser c,
        AccessPolicy p,
        DtoMapper m,
        JdbcTemplate jdbc,
        @Value("${gmao.public-origin}") String origin
    ) {
        store = s;
        current = c;
        policy = p;
        mapper = m;
        this.jdbc = jdbc;
        this.origin = origin;
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Object upload(
        @RequestParam String targetType,
        @RequestParam UUID targetId,
        @RequestParam MultipartFile file
    ) throws Exception {
        var target = store.find(policy.typeForTarget(targetType), targetId);
        policy.read(target);
        ApiException.require(
            policy.supervisor() ||
                current.get().id.equals(target.createdBy) ||
                (target instanceof Asset && current.has("TECHNICIEN")) ||
                target instanceof tn.lamg.gmao.modules.maintenance.model.WorkOrder,
            "FILE_FORBIDDEN",
            "Ajout de document non autorisé."
        );
        if (target instanceof tn.lamg.gmao.modules.maintenance.model.WorkOrder w) policy.execute(w);
        ApiException.require(
            !file.isEmpty() && file.getSize() <= 25 * 1024 * 1024,
            "FILE_SIZE",
            "Fichier vide ou supérieur à 25 Mio."
        );
        byte[] bytes = file.getBytes();
        String type = detect(bytes);
        ApiException.require(
            type != null,
            "FILE_TYPE",
            "Formats acceptés : PDF, JPEG, PNG et WebP."
        );
        var a = new Attachment();
        a.targetType = targetType;
        a.targetId = targetId;
        a.name = Objects.requireNonNullElse(file.getOriginalFilename(), "document").replaceAll(
            "[^\\p{L}\\p{N}._ -]",
            "_"
        );
        if (a.name.length() > 200) a.name = a.name.substring(a.name.length() - 200);
        a.contentType = type;
        a.size = file.getSize();
        a.createdBy = current.get().id;
        store.save(a);
        jdbc.update(
            "insert into attachment_content (attachment_id, content) values (?, ?)",
            a.id,
            bytes
        );
        return mapper.dto(a);
    }

    @GetMapping("/files/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        var a = store.find(Attachment.class, id);
        policy.read(a);
        byte[] content = jdbc.queryForObject(
            "select content from attachment_content where attachment_id = ?",
            byte[].class,
            id
        );
        return ResponseEntity.ok()
            .header("X-Content-Type-Options", "nosniff")
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename(a.name, java.nio.charset.StandardCharsets.UTF_8)
                    .build()
                    .toString()
            )
            .contentType(MediaType.parseMediaType(a.contentType))
            .body(content);
    }

    @GetMapping(value = "/assets/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qr(@PathVariable UUID id) throws Exception {
        var asset = store.find(Asset.class, id);
        policy.read(asset);
        var matrix = new QRCodeWriter().encode(
            origin + "/scan?code=" + asset.qrToken,
            BarcodeFormat.QR_CODE,
            320,
            320
        );
        var out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    @GetMapping("/assets/by-qr/{token}")
    public Object resolve(@PathVariable UUID token) {
        var a = store
            .all(Asset.class)
            .stream()
            .filter(x -> token.equals(x.qrToken))
            .findFirst()
            .orElseThrow(() -> new ApiException(404, "NOT_FOUND", "QR inconnu."));
        policy.read(a);
        return mapper.dto(a);
    }

    static String detect(byte[] b) {
        if (b.length < 12) return null;
        if (b[0] == (byte) 0xff && b[1] == (byte) 0xd8 && b[2] == (byte) 0xff) return "image/jpeg";
        if (b[0] == (byte) 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if (
            new String(b, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")
        ) return "application/pdf";
        if (
            new String(b, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF") &&
            new String(b, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")
        ) return "image/webp";
        return null;
    }
}
