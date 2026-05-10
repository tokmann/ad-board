package com.adboard.unit.service;

import com.adboard.dto.mapper.UserMapper;
import com.adboard.dto.request.user.ProfileUpdateRequestDto;
import com.adboard.dto.response.user.UserProfileDto;
import com.adboard.entity.User;
import com.adboard.entity.reference.Role;
import com.adboard.exception.ResourceNotFoundException;
import com.adboard.repository.UserRepository;
import com.adboard.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @InjectMocks
  private UserService userService;

  private User user;
  private UserProfileDto profileDto;
  private ProfileUpdateRequestDto updateRequest;
  private Authentication auth;
  private Role roleUser;

  @BeforeEach
  void setUp() {
    roleUser = new Role();
    roleUser.setName("ROLE_USER");

    user = new User();
    user.setId(1L);
    user.setEmail("user@test.com");
    user.setUsername("Пользователь");
    user.setPasswordHash("hashed");
    user.setRoles(Set.of(roleUser));

    profileDto = new UserProfileDto();
    profileDto.setEmail("user@test.com");
    profileDto.setUsername("Пользователь");

    updateRequest = new ProfileUpdateRequestDto();
    updateRequest.setUsername("Новое имя");
    updateRequest.setPhone("+123456789");

    auth = mock(Authentication.class);
  }

  /**
   * Testing getMyProfile
   * */

  @Test
  @DisplayName("Should return UserProfileDto when user exists")
  void getMyProfile_returnsUserProfile_whenUserExists() {
    when(auth.getName()).thenReturn("user@test.com");
    when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    when(userMapper.toProfileDto(user)).thenReturn(profileDto);

    UserProfileDto result = userService.getMyProfile(auth);

    assertThat(result.getEmail()).isEqualTo("user@test.com");
    verify(userRepository).findByEmail("user@test.com");
  }

  @Test
  @DisplayName("Should throw exception when user is not found")
  void getMyProfile_throws_whenUserNotFound() {
    when(auth.getName()).thenReturn("user@test.com");
    when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getMyProfile(auth))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  /**
   * Testing updateMyProfile
   * */

  @Test
  @DisplayName("Should update user fields and return updated UserProfileDto")
  void updateMyProfile_updatesFieldsAndReturnsDto_whenValid() {
    when(auth.getName()).thenReturn("user@test.com");
    when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    when(userMapper.toProfileDto(user)).thenReturn(profileDto);
    doAnswer(invocation -> {
      ProfileUpdateRequestDto dto = invocation.getArgument(0);
      User target = invocation.getArgument(1);
      target.setUsername(dto.getUsername());
      target.setPhone(dto.getPhone());
      return null;
    }).when(userMapper).updateEntityFromDto(any(ProfileUpdateRequestDto.class), any(User.class));


    userService.updateMyProfile(updateRequest, auth);

    verify(userMapper).updateEntityFromDto(updateRequest, user);
    verify(userRepository).save(user);
    assertThat(user.getUsername()).isEqualTo("Новое имя");
    assertThat(user.getPhone()).isEqualTo("+123456789");
  }

  @Test
  @DisplayName("Should throw exception when updating non-existent user")
  void updateMyProfile_throws_whenUserNotFound() {
    when(auth.getName()).thenReturn("user@test.com");
    when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.updateMyProfile(updateRequest, auth))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(userRepository, never()).save(any());
  }

  /**
   * Testing loadByUsername
   * */

  @Test
  @DisplayName("Should return UserDetails with authorities for Spring Security")
  void loadUserByUsername_returnsUserDetailsWithAuthorities_whenUserExists() {
    when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

    UserDetails result = userService.loadUserByUsername("user@test.com");

    assertThat(result.getUsername()).isEqualTo("user@test.com");
    assertThat(result.getPassword()).isEqualTo("hashed");
    assertThat(result.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_USER");
  }

  @Test
  @DisplayName("Should throw exception for Spring Security when user not found")
  void loadUserByUsername_throws_whenUserNotFound() {
    when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.loadUserByUsername("unknown@test.com"))
        .isInstanceOf(UsernameNotFoundException.class);
  }
}