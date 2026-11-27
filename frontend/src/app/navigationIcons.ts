import AddShoppingCartOutlined from '@mui/icons-material/AddShoppingCartOutlined';
import AssignmentIndOutlined from '@mui/icons-material/AssignmentIndOutlined';
import AttachFileOutlined from '@mui/icons-material/AttachFileOutlined';
import BookmarkBorderOutlined from '@mui/icons-material/BookmarkBorderOutlined';
import BusinessOutlined from '@mui/icons-material/BusinessOutlined';
import ChecklistOutlined from '@mui/icons-material/ChecklistOutlined';
import CompareArrowsOutlined from '@mui/icons-material/CompareArrowsOutlined';
import ConstructionOutlined from '@mui/icons-material/ConstructionOutlined';
import DescriptionOutlined from '@mui/icons-material/DescriptionOutlined';
import EngineeringOutlined from '@mui/icons-material/EngineeringOutlined';
import ErrorOutline from '@mui/icons-material/ErrorOutline';
import EventAvailableOutlined from '@mui/icons-material/EventAvailableOutlined';
import EventBusyOutlined from '@mui/icons-material/EventBusyOutlined';
import EventRepeatOutlined from '@mui/icons-material/EventRepeatOutlined';
import FactCheckOutlined from '@mui/icons-material/FactCheckOutlined';
import GroupsOutlined from '@mui/icons-material/GroupsOutlined';
import HistoryOutlined from '@mui/icons-material/HistoryOutlined';
import HomeRepairServiceOutlined from '@mui/icons-material/HomeRepairServiceOutlined';
import Inventory2Outlined from '@mui/icons-material/Inventory2Outlined';
import LocationOnOutlined from '@mui/icons-material/LocationOnOutlined';
import NotificationsActiveOutlined from '@mui/icons-material/NotificationsActiveOutlined';
import PauseCircleOutline from '@mui/icons-material/PauseCircleOutline';
import PeopleOutline from '@mui/icons-material/PeopleOutline';
import PrecisionManufacturingOutlined from '@mui/icons-material/PrecisionManufacturingOutlined';
import PsychologyOutlined from '@mui/icons-material/PsychologyOutlined';
import ReportProblemOutlined from '@mui/icons-material/ReportProblemOutlined';
import ScheduleOutlined from '@mui/icons-material/ScheduleOutlined';
import ShoppingCartOutlined from '@mui/icons-material/ShoppingCartOutlined';
import StorefrontOutlined from '@mui/icons-material/StorefrontOutlined';
import SwapHorizOutlined from '@mui/icons-material/SwapHorizOutlined';
import TimerOutlined from '@mui/icons-material/TimerOutlined';
import VerifiedUserOutlined from '@mui/icons-material/VerifiedUserOutlined';
import type { SvgIconComponent } from '@mui/icons-material';

export const resourceIcons: Record<string, SvgIconComponent> = {
  locations: LocationOnOutlined,
  assets: PrecisionManufacturingOutlined,
  attachments: AttachFileOutlined,
  teams: GroupsOutlined,
  skills: PsychologyOutlined,
  'user-skills': VerifiedUserOutlined,
  'team-members': PeopleOutline,
  shifts: ScheduleOutlined,
  'shift-assignments': AssignmentIndOutlined,
  absences: EventBusyOutlined,
  handovers: SwapHorizOutlined,
  checklists: ChecklistOutlined,
  'preventive-plans': EventRepeatOutlined,
  'preventive-occurrences': EventAvailableOutlined,
  'work-orders': HomeRepairServiceOutlined,
  'failure-reports': ReportProblemOutlined,
  'time-entries': TimerOutlined,
  anomalies: ErrorOutline,
  downtimes: PauseCircleOutline,
  suppliers: StorefrontOutlined,
  'inventory/items': Inventory2Outlined,
  'inventory/reservations': BookmarkBorderOutlined,
  'inventory/movements': CompareArrowsOutlined,
  'purchase-requests': AddShoppingCartOutlined,
  'purchase-orders': ShoppingCartOutlined,
  'goods-receipts': FactCheckOutlined,
  subcontractors: BusinessOutlined,
  contracts: DescriptionOutlined,
  'external-interventions': EngineeringOutlined,
  'material-requests': ConstructionOutlined,
  notifications: NotificationsActiveOutlined,
  'audit-events': HistoryOutlined,
};
