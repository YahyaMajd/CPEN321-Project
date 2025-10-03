# Role Separation Feature - Developer Documentation

## Overview

The Role Separation feature implements a comprehensive user role-based navigation system for DormDash, allowing users to select between **STUDENT** and **MOVER** roles after authentication, with intelligent routing to role-specific screens and functionality.

## Architecture

### Core Components

#### 1. **Authentication Flow**
- **Google OAuth Integration**: Uses Google Sign-In API for user authentication
- **JWT Token Management**: 19-hour expiry tokens stored in encrypted DataStore
- **Token Validation**: Automatic token verification on app launch

#### 2. **Role Management System**
- **Two-Phase Signup**: Authentication → Role Selection → Profile Completion (optional)
- **Role Persistence**: User roles stored in MongoDB and cached in navigation state
- **Role-Based Routing**: Intelligent navigation to role-specific screens

#### 3. **Navigation Architecture**
- **Event-Driven Navigation**: NavigationStateManager handles all routing decisions
- **State Management**: Centralized navigation state with role tracking
- **Smart Routing**: Context-aware navigation based on user state

## Implementation Details

### Backend Integration

#### API Endpoints
```typescript
POST /api/auth/signup        // Google OAuth signup
POST /api/auth/signin        // Google OAuth signin  
POST /api/auth/select-role   // Role selection (JWT protected)
GET  /api/user/profile       // Get current user data
```

#### Data Models
```typescript
// User Schema (MongoDB)
interface IUser {
  email: string;
  name: string;
  profilePicture: string;
  userRole?: 'STUDENT' | 'MOVER';  // Optional for two-phase signup
  bio?: string;
  hobbies: string[];
  createdAt: Date;
  updatedAt: Date;
}

// Role Selection Request
interface SelectRoleRequest {
  userRole: 'STUDENT' | 'MOVER';
}
```

#### Authentication Middleware
```typescript
// JWT token validation for role selection
authenticateToken → validateData(selectRoleSchema) → selectRole handler
```

### Frontend Implementation

#### Key Files & Responsibilities

| File | Responsibility |
|------|----------------|
| `AuthViewModel.kt` | Authentication logic, role selection API calls |
| `NavigationStateManager.kt` | Centralized routing logic and state management |
| `Navigation.kt` | Navigation event handling and route definitions |
| `AuthRepository.kt` | Backend API communication layer |
| `RoleSelectionScreen.kt` | Role selection UI component |
| `StudentMainScreen.kt` | Student-specific main screen |
| `MoverMainScreen.kt` | Mover-specific main screen |

#### Navigation State Management
```kotlin
data class NavigationState(
    val isAuthenticated: Boolean = false,
    val needsProfileCompletion: Boolean = false,
    val needsRoleSelection: Boolean = false,
    val userRole: String? = null,  // Critical for role-based routing
    val currentRoute: String = NavRoutes.LOADING
)
```

#### Role Selection Flow
```kotlin
// 1. User selects role in UI
RoleSelectionScreen { role -> authViewModel.selectUserRole(role) }

// 2. API call to backend
authRepository.selectUserRole(role) // POST /api/auth/select-role

// 3. Update navigation state with role
navigationStateManager.handleRoleSelectionWithMessage(
    userRole = role,
    message = "Welcome! Your role has been set to ${role}",
    needsProfileCompletion = user.bio.isNullOrBlank()
)

// 4. Smart routing based on user state
when (userRole.uppercase()) {
    "STUDENT" -> navigateToStudentMainWithMessage(message)
    "MOVER" -> navigateToMoverMainWithMessage(message)
    else -> navigateToMainWithMessage(message)
}
```

## User Flow Scenarios

### Scenario 1: New User (Complete Flow)
```
App Launch → Loading → Auth Check (not authenticated) → AuthScreen → 
Google OAuth → Role Selection → Profile Completion → StudentMainScreen/MoverMainScreen
```

### Scenario 2: Returning User (Skip role selection)
```
App Launch → Loading → Auth Check (authenticated + has role) → 
StudentMainScreen/MoverMainScreen
```

### Scenario 3: User Without Role (Role selection required)
```
App Launch → Loading → Auth Check (authenticated + no role) → 
RoleSelectionScreen → StudentMainScreen/MoverMainScreen
```

### Scenario 4: Profile Completion After Role Selection
```
Role Selection → API Success → Profile Completion Required → 
ProfileCompletionScreen → Complete Bio → StudentMainScreen/MoverMainScreen
```

## Critical Implementation Details

### 1. **Profile Completion Bug Fix**
**Problem**: Profile completion was always routing to generic `MainScreen`
**Solution**: Enhanced profile completion handlers to check stored `userRole` and route to role-specific screens

```kotlin
// Before (Broken)
fun handleProfileCompletion() {
    navigateToMain() // Always generic screen
}

// After (Fixed)  
fun handleProfileCompletion() {
    navigateToRoleBasedMainScreen() // Check role and route accordingly
}
```

### 2. **State Persistence Strategy**
- **JWT Tokens**: Stored in encrypted Android DataStore
- **User Role**: Cached in NavigationState for fast routing decisions
- **Navigation Context**: Preserved throughout profile completion flow

### 3. **Error Handling**
- **Network Failures**: User-friendly error messages with retry capability
- **Token Expiration**: Automatic re-authentication flow
- **Invalid Roles**: Server-side validation prevents invalid role selection
- **Fallback Routing**: Generic MainScreen as safety net

## Testing Strategy

### Unit Tests
- AuthViewModel role selection logic
- NavigationStateManager routing decisions
- AuthRepository API communication

### Integration Tests
- End-to-end authentication flow
- Role selection with profile completion
- Navigation state persistence

### Test Scenarios
```kotlin
// Test role selection without profile completion
@Test fun `selectUserRole_withCompletedProfile_navigatesToRoleSpecificScreen`()

// Test role selection with profile completion  
@Test fun `selectUserRole_withIncompleteProfile_navigatesToProfileCompletion`()

// Test profile completion routing
@Test fun `handleProfileCompletion_withStoredRole_navigatesToRoleSpecificScreen`()
```

## Security Considerations

### Authentication Security
- **Google OAuth**: Secure third-party authentication
- **JWT Tokens**: Signed tokens with expiration
- **Token Storage**: Encrypted local storage (DataStore)
- **API Protection**: Role selection requires valid JWT

### Authorization
- **Role-Based Access**: Different screens for different roles
- **API Authorization**: Backend validates user roles
- **State Protection**: Navigation state prevents unauthorized access

## Performance Optimizations

### State Management
- **Centralized State**: Single source of truth for navigation
- **Efficient Updates**: Only trigger navigation when state changes
- **Memory Management**: Proper ViewModel lifecycle management

### Network Optimization
- **Token Caching**: Avoid unnecessary authentication calls
- **Smart Routing**: Minimize API calls during navigation
- **Error Recovery**: Graceful handling of network failures

## Future Enhancements

### Role-Specific Features
```kotlin
// Planned role-specific functionality
when (userRole) {
    "STUDENT" -> {
        // Student-specific features:
        // - Browse moving services
        // - Request help with moving
        // - Rate and review movers
    }
    "MOVER" -> {
        // Mover-specific features:
        // - Create service listings
        // - Manage bookings
        // - Track earnings
        // - Availability management
    }
}
```

### Additional Roles
- Easy extension to support additional roles (e.g., "ADMIN", "BUSINESS")
- Role hierarchy and permissions system
- Dynamic role assignment

### Analytics Integration
- Track role selection patterns
- Monitor user flow drop-off points
- A/B test role selection UI variations

## Troubleshooting

### Common Issues

1. **User stuck on MainScreen instead of role-specific screen**
   - Check `NavigationState.userRole` is properly set
   - Verify profile completion handlers use `navigateToRoleBasedMainScreen()`

2. **Role selection API calls failing**
   - Verify JWT token is valid and not expired
   - Check backend `/api/auth/select-role` endpoint is accessible
   - Validate request body format matches `SelectRoleRequest`

3. **Navigation state not persisting**
   - Ensure `NavigationStateManager` is singleton
   - Verify role is stored in navigation state during selection
   - Check DataStore token persistence

### Debug Tools
```kotlin
// Add logging to track navigation state
Log.d("Navigation", "Current state: ${navigationState.value}")
Log.d("Navigation", "User role: ${navigationState.value.userRole}")
Log.d("Navigation", "Route: ${navigationState.value.currentRoute}")
```

## Code Review Checklist

- [ ] Role selection updates navigation state with user role
- [ ] Profile completion handlers check user role before routing
- [ ] API calls include proper error handling
- [ ] Navigation events are properly cleared after handling
- [ ] JWT tokens are securely stored and validated
- [ ] Role-specific screens are properly integrated in NavHost
- [ ] Success messages are displayed for role selection
- [ ] Fallback routing handles edge cases

---

**Version**: 1.0  
**Last Updated**: October 2025  
**Authors**: Development Team  
**Dependencies**: Jetpack Compose, Hilt, Retrofit, DataStore, Google Sign-In