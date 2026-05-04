package com.assignment.service;

import com.assignment.entity.Post;
import com.assignment.entity.Comment;
import com.assignment.entity.User;
import com.assignment.entity.Bot;
import com.assignment.repository.PostRepository;
import com.assignment.repository.CommentRepository;
import com.assignment.repository.UserRepository;
import com.assignment.repository.BotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PostService {
    private static final Logger logger = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BotRepository botRepository;
    private final RedisGuardrailsService redisGuardrailsService;
    private final NotificationService notificationService;

    public PostService(PostRepository postRepository,
                      CommentRepository commentRepository,
                      UserRepository userRepository,
                      BotRepository botRepository,
                      RedisGuardrailsService redisGuardrailsService,
                      NotificationService notificationService) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.botRepository = botRepository;
        this.redisGuardrailsService = redisGuardrailsService;
        this.notificationService = notificationService;
    }

    @Transactional
    public Post createPost(Long authorId, String content) {
        Post post = new Post();
        post.setAuthorId(authorId);
        post.setContent(content);
        Post saved = postRepository.save(post);
        logger.info("Post created with ID: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Comment addComment(Long postId, Long authorId, String content, Integer depthLevel) {
        if (!redisGuardrailsService.checkVerticalCap(depthLevel)) {
            throw new RuntimeException("Vertical cap exceeded for depth level: " + depthLevel);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        boolean isBot = checkIfBot(authorId);

        if (isBot) {
            if (!redisGuardrailsService.checkAndIncrementBotCount(postId)) {
                throw new RuntimeException("Horizontal cap exceeded for post: " + postId);
            }

            Long humanAuthorId = post.getAuthorId();
            Bot bot = botRepository.findById(authorId)
                    .orElseThrow(() -> new RuntimeException("Bot not found"));

            if (!redisGuardrailsService.checkAndSetCooldown(bot.getId(), humanAuthorId)) {
                throw new RuntimeException("Cooldown active for bot: " + bot.getId());
            }

            redisGuardrailsService.incrementViralityScore(postId, "BOT_REPLY");

            User human = userRepository.findById(humanAuthorId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            notificationService.handleNotification(human.getId(), bot.getName());
        } else {
            redisGuardrailsService.incrementViralityScore(postId, "HUMAN_COMMENT");
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(authorId);
        comment.setContent(content);
        comment.setDepthLevel(depthLevel);

        Comment saved = commentRepository.save(comment);
        logger.info("Comment created with ID: {} for post: {}", saved.getId(), postId);
        return saved;
    }

    @Transactional
    public void likePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);

        redisGuardrailsService.incrementViralityScore(postId, "HUMAN_LIKE");
        redisGuardrailsService.incrementLikeCount(postId);

        logger.info("Post {} liked by user {}", postId, userId);
    }

    private boolean checkIfBot(Long id) {
        return botRepository.findById(id).isPresent();
    }

    public Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    public java.util.List<Comment> getCommentsByPost(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    public Long getBotCommentCount(Long postId) {
        return redisGuardrailsService.getBotCountForPost(postId);
    }

    public Long getPostViralityScore(Long postId) {
        return redisGuardrailsService.getViralityScore(postId);
    }
}
