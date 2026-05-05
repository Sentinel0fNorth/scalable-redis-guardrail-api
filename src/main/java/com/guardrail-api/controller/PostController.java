package com.assignment.controller;

import com.assignment.entity.Post;
import com.assignment.entity.Comment;
import com.assignment.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPost(@RequestBody PostRequest request) {
        try {
            Post post = postService.createPost(request.getAuthorId(), request.getContent());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", post);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long postId,
            @RequestBody CommentRequest request) {
        try {
            Integer depthLevel = request.getDepthLevel() != null ? request.getDepthLevel() : 0;
            Comment comment = postService.addComment(postId, request.getAuthorId(), 
                    request.getContent(), depthLevel);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", comment);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());

            if (e.getMessage().contains("Horizontal cap exceeded")) {
                return ResponseEntity.status(429).body(response);
            }
            if (e.getMessage().contains("Vertical cap exceeded")) {
                return ResponseEntity.status(400).body(response);
            }
            if (e.getMessage().contains("Cooldown active")) {
                return ResponseEntity.status(429).body(response);
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Object>> likePost(
            @PathVariable Long postId,
            @RequestBody LikeRequest request) {
        try {
            postService.likePost(postId, request.getUserId());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Post liked successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> getPost(@PathVariable Long postId) {
        try {
            Post post = postService.getPost(postId);
            Long botCount = postService.getBotCommentCount(postId);
            Long viralityScore = postService.getPostViralityScore(postId);

            Map<String, Object> postData = new HashMap<>();
            postData.put("id", post.getId());
            postData.put("authorId", post.getAuthorId());
            postData.put("content", post.getContent());
            postData.put("createdAt", post.getCreatedAt());
            postData.put("likeCount", post.getLikeCount());
            postData.put("botCommentCount", botCount);
            postData.put("viralityScore", viralityScore);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", postData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    static class PostRequest {
        private Long authorId;
        private String content;

        public Long getAuthorId() {
            return authorId;
        }

        public void setAuthorId(Long authorId) {
            this.authorId = authorId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    static class CommentRequest {
        private Long authorId;
        private String content;
        private Integer depthLevel;

        public Long getAuthorId() {
            return authorId;
        }

        public void setAuthorId(Long authorId) {
            this.authorId = authorId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getDepthLevel() {
            return depthLevel;
        }

        public void setDepthLevel(Integer depthLevel) {
            this.depthLevel = depthLevel;
        }
    }

    static class LikeRequest {
        private Long userId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
}
