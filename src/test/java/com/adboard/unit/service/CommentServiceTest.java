package com.adboard.unit.service;

import com.adboard.dto.mapper.CommentMapper;
import com.adboard.dto.request.comment.CommentRequestDto;
import com.adboard.dto.response.PageResponse;
import com.adboard.dto.response.comment.CommentResponseDto;
import com.adboard.entity.Ad;
import com.adboard.entity.Comment;
import com.adboard.entity.User;
import com.adboard.exception.ResourceNotFoundException;
import com.adboard.exception.UnauthorizedActionException;
import com.adboard.repository.AdRepository;
import com.adboard.repository.CommentRepository;
import com.adboard.repository.UserRepository;
import com.adboard.service.CommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @Mock
  private CommentRepository commentRepository;

  @Mock
  private AdRepository adRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CommentMapper commentMapper;

  @InjectMocks
  private CommentService commentService;

  private Ad ad;
  private User author;
  private Comment comment;
  private CommentResponseDto commentDto;
  private CommentRequestDto request;
  private Authentication auth;

  @BeforeEach
  void setUp() {
    ad = new Ad();
    ad.setId(1L);

    author = new User();
    author.setId(10L);
    author.setEmail("user@test.com");

    comment = new Comment();
    comment.setId(100L);
    comment.setAd(ad);
    comment.setAuthor(author);
    comment.setText("Тестовый комментарий");

    commentDto = new CommentResponseDto();
    commentDto.setId(100L);
    commentDto.setText("Тестовый комментарий");

    request = new CommentRequestDto();
    request.setText("Тестовый комментарий");

    auth = mock(Authentication.class);
  }

  /**
   * Testing getCommentsByAdId
   * */

  @Test
  @DisplayName("Should return paginated comments with nested replies")
  void getCommentsByAdId_returnsPaginatedCommentsWithReplies() {
    Comment root1 = new Comment();
    root1.setId(100L);
    root1.setAd(ad);
    root1.setAuthor(author);
    root1.setText("Корень 1");
    root1.setParentComment(null);

    Comment root2 = new Comment();
    root2.setId(101L);
    root2.setAd(ad);
    root2.setAuthor(author);
    root2.setText("Корень 2");
    root2.setParentComment(null);

    Comment reply1 = new Comment();
    reply1.setId(200L);
    reply1.setAd(ad);
    reply1.setAuthor(author);
    reply1.setText("Ответ на корень 1");
    reply1.setParentComment(root1);

    Comment reply2 = new Comment();
    reply2.setId(201L);
    reply2.setAd(ad);
    reply2.setAuthor(author);
    reply2.setText("Ответ на корень 1 #2");
    reply2.setParentComment(root1);

    Comment nestedReply = new Comment();
    nestedReply.setId(300L);
    nestedReply.setAd(ad);
    nestedReply.setAuthor(author);
    nestedReply.setText("Вложенный ответ");
    nestedReply.setParentComment(reply1);

    CommentResponseDto root1Dto = new CommentResponseDto();
    root1Dto.setId(100L);
    root1Dto.setText("Корень 1");

    CommentResponseDto root2Dto = new CommentResponseDto();
    root2Dto.setId(101L);
    root2Dto.setText("Корень 2");

    CommentResponseDto reply1Dto = new CommentResponseDto();
    reply1Dto.setId(200L);
    reply1Dto.setText("Ответ на корень 1");

    CommentResponseDto reply2Dto = new CommentResponseDto();
    reply2Dto.setId(201L);
    reply2Dto.setText("Ответ на корень 1 #2");

    CommentResponseDto nestedDto = new CommentResponseDto();
    nestedDto.setId(300L);
    nestedDto.setText("Вложенный ответ");

    when(adRepository.findById(1L)).thenReturn(Optional.of(ad));
    when(commentRepository.findAllByAdId(1L)).thenReturn(List.of(root1, root2, reply1, reply2, nestedReply));

    when(commentMapper.toResponseDto(root1)).thenReturn(root1Dto);
    when(commentMapper.toResponseDto(root2)).thenReturn(root2Dto);
    when(commentMapper.toResponseDto(reply1)).thenReturn(reply1Dto);
    when(commentMapper.toResponseDto(reply2)).thenReturn(reply2Dto);
    when(commentMapper.toResponseDto(nestedReply)).thenReturn(nestedDto);

    PageResponse<CommentResponseDto> result = commentService.getCommentsByAdId(1L, 0, 10);

    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getTotalElements()).isEqualTo(2);

    CommentResponseDto foundRoot1 = result.getContent().stream()
        .filter(dto -> dto.getText().equals("Корень 1"))
        .findFirst()
        .orElseThrow();

    assertThat(foundRoot1.getReplies()).hasSize(2);
    assertThat(foundRoot1.getReplies().stream().map(CommentResponseDto::getText))
        .containsExactlyInAnyOrder("Ответ на корень 1", "Ответ на корень 1 #2");

    CommentResponseDto foundReply1 = foundRoot1.getReplies().stream()
        .filter(dto -> dto.getText().equals("Ответ на корень 1"))
        .findFirst()
        .orElseThrow();

    assertThat(foundReply1.getReplies()).hasSize(1);
    assertThat(foundReply1.getReplies().get(0).getText()).isEqualTo("Вложенный ответ");
  }

  @Test
  @DisplayName("Should throw exception when ad is not found")
  void getCommentsByAdId_throws_whenAdNotFound() {
    when(adRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentService.getCommentsByAdId(999L, 0, 10))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  /**
   * Testing addComment
   * */

  @Test
  @DisplayName("Should create root comment when parentCommentId is null")
  void addComment_createsRootComment_whenValid() {
    when(auth.getName()).thenReturn("user@test.com");
    when(adRepository.findById(1L)).thenReturn(Optional.of(ad));
    when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(author));
    when(commentMapper.toEntity(request)).thenReturn(comment);
    when(commentMapper.toResponseDto(comment)).thenReturn(commentDto);

    commentService.addComment(1L, request, auth);

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    Comment saved = captor.getValue();

    assertThat(saved.getAd()).isEqualTo(ad);
    assertThat(saved.getAuthor()).isEqualTo(author);
    assertThat(saved.getParentComment()).isNull();
  }

  @Test
  @DisplayName("Should create reply when parentCommentId is provided and valid")
  void addComment_createsReply_whenParentIdProvided() {
    when(auth.getName()).thenReturn("user@test.com");

    Comment parent = new Comment();
    parent.setId(50L);
    parent.setAd(ad);

    request.setParentCommentId(50L);

    when(adRepository.findById(1L)).thenReturn(Optional.of(ad));
    when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(author));
    when(commentRepository.findByIdWithAuthor(50L)).thenReturn(Optional.of(parent));
    when(commentMapper.toEntity(request)).thenReturn(comment);
    when(commentMapper.toResponseDto(comment)).thenReturn(commentDto);

    commentService.addComment(1L, request, auth);

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).save(captor.capture());
    assertThat(captor.getValue().getParentComment()).isEqualTo(parent);
  }

  @Test
  @DisplayName("Should throw exception when parent comment belongs to another ad")
  void addComment_throws_whenParentCommentBelongsToDifferentAd() {
    when(auth.getName()).thenReturn("user@test.com");

    Ad otherAd = new Ad();
    otherAd.setId(2L);
    Comment parent = new Comment();
    parent.setId(50L);
    parent.setAd(otherAd);

    request.setParentCommentId(50L);

    when(adRepository.findById(1L)).thenReturn(Optional.of(ad));
    when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(author));
    when(commentRepository.findByIdWithAuthor(50L)).thenReturn(Optional.of(parent));

    assertThatThrownBy(() -> commentService.addComment(1L, request, auth))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should throw exception when ad is not found (at creation of comment)")
  void addComment_throws_whenAdNotFound() {
    when(auth.getName()).thenReturn("user@test.com");
    when(adRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> commentService.addComment(999L, request, auth))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  /**
   * Testing deleteComment
   * */

  @Test
  @DisplayName("Should allow author to delete their own comment")
  void deleteComment_owner_canDelete() {
    when(auth.getName()).thenReturn("user@test.com");
    when(commentRepository.findByIdWithAuthor(100L)).thenReturn(Optional.of(comment));

    commentService.deleteComment(1L, 100L, auth);

    verify(commentRepository).delete(comment);
  }

  @Test
  @DisplayName("Should throw exception when non-author tries to delete")
  void deleteComment_throws_whenNotOwner() {
    when(auth.getName()).thenReturn("user@test.com");

    User other = new User();
    other.setEmail("other@test.com");
    comment.setAuthor(other);

    when(commentRepository.findByIdWithAuthor(100L)).thenReturn(Optional.of(comment));

    assertThatThrownBy(() -> commentService.deleteComment(1L, 100L, auth))
        .isInstanceOf(UnauthorizedActionException.class);
  }
}