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

  private Ad ad;
  private User author;
  private Comment comment;
  private CommentResponseDto commentDto;
  private CommentRequestDto request;
  private Authentication auth;

  @InjectMocks
  private CommentService commentService;

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
    comment.setText("Test comment");

    commentDto = new CommentResponseDto();
    commentDto.setId(100L);
    commentDto.setText("Test comment");

    request = new CommentRequestDto();
    request.setText("Test comment");

    auth = mock(Authentication.class);
  }

  /**
   * Testing getCommentsByAdId
   * */

  @Test
  @DisplayName("Should return paginated comments with nested replies")
  void getCommentsByAdId_returnsPaginatedCommentsWithReplies() {
    Comment reply = new Comment();
    reply.setId(101L);
    reply.setParentComment(comment);
    CommentResponseDto replyDto = new CommentResponseDto();
    replyDto.setId(101L);

    when(adRepository.findById(1L)).thenReturn(Optional.of(ad));
    when(commentRepository.countRootByAdId(1L)).thenReturn(1L);
    when(commentRepository.findRootByAdId(1L, 0, 10)).thenReturn(List.of(comment));
    when(commentMapper.toResponseDto(comment)).thenReturn(commentDto);
    when(commentRepository.findRepliesByParentId(100L)).thenReturn(List.of(reply));
    when(commentMapper.toResponseDto(reply)).thenReturn(replyDto);

    PageResponse<CommentResponseDto> result = commentService.getCommentsByAdId(1L, 0, 10);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getReplies()).hasSize(1);
    assertThat(result.getTotalElements()).isEqualTo(1);
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