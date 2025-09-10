# Notification Panel UI Fixes

## Problems Fixed

### 1. Silent Title Clipping Issue
**Problem**: The silent notification title was being clipped by the top padding in the notification info layouts, making the text partially hidden.

**Root Cause**: The silent notification button was using a single `android:padding` attribute which applied the same padding to all sides, causing the top padding to clip the title text.

**Solution**: Replaced the single `android:padding` attribute with individual padding attributes:
- `android:paddingTop="@dimen/notification_importance_button_padding"`
- `android:paddingBottom="@dimen/notification_importance_button_padding"`
- `android:paddingStart="@dimen/notification_importance_button_padding"`
- `android:paddingEnd="@dimen/notification_importance_button_padding"`

### 2. Grouped Message Width Mismatch
**Problem**: The width of grouped notification messages didn't match the header message, causing visual misalignment.

**Root Cause**: The notification children container didn't have proper content margins to align with the header notification's content area.

**Solution**: Added proper content margins to the notification children container:
- `android:paddingStart="@*android:dimen/notification_content_margin_start"`
- `android:paddingEnd="@*android:dimen/notification_content_margin_end"`

## Files Modified

### Silent Title Clipping Fixes
- **Modified**: `packages/SystemUI/res/layout/notification_info.xml`
  - Line 283-286: Updated silent button padding attributes
- **Modified**: `packages/SystemUI/res/layout/bundle_notification_info.xml`
  - Line 272-275: Updated silent button padding attributes
- **Modified**: `packages/SystemUI/res/layout/notification_conversation_info.xml`
  - Line 334-337: Updated silent button padding attributes

### Grouped Message Width Fix
- **Modified**: `packages/SystemUI/res/layout/notification_children_container.xml`
  - Line 22-23: Added content margin padding to align with header notifications

## Technical Details

### Padding Dimensions Used
- `notification_importance_button_padding`: 16dp (from SystemUI dimens)
- `notification_content_margin_start`: 52dp (from core framework)
- `notification_content_margin_end`: 16dp (from core framework)

### Impact
- **Silent Title Fix**: Ensures silent notification titles are fully visible and not clipped
- **Width Alignment Fix**: Ensures grouped notification messages align properly with header messages
- **Visual Consistency**: Improves overall notification panel appearance and usability
- **No Breaking Changes**: All changes maintain existing functionality while fixing visual issues

## Testing
- No linting errors detected in modified files
- Changes follow Android layout best practices
- Uses existing dimension resources for consistency

## Date
2024-12-19
