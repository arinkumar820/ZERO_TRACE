<?php
/**
 * Firebase User Status Verification for Bisto Chat
 * 
 * This PHP script verifies user status in Firebase Realtime Database
 * before allowing message insertion into MySQL database.
 * 
 * Requirements:
 * - Firebase Admin SDK for PHP
 * - MySQL PDO extension
 * - Composer autoload
 * 
 * Install Firebase Admin SDK:
 * composer require google/cloud-firestore
 * composer require kreait/firebase-php
 */

require_once 'vendor/autoload.php';

use Kreait\Firebase\Factory;
use Kreait\Firebase\ServiceAccount;

class FirebaseUserVerification {
    
    private $firebase;
    private $database;
    private $mysql_pdo;
    
    // Firebase configuration
    private $firebase_credentials_path = 'path/to/your/firebase-service-account.json';
    private $firebase_database_url = 'https://your-project-default-rtdb.firebaseio.com/';
    
    // MySQL configuration
    private $mysql_host = 'localhost';
    private $mysql_db = 'bisto_chat';
    private $mysql_user = 'your_username';
    private $mysql_pass = 'your_password';
    
    public function __construct() {
        $this->initializeFirebase();
        $this->initializeMySQL();
    }
    
    /**
     * Initialize Firebase Admin SDK
     */
    private function initializeFirebase() {
        try {
            $serviceAccount = ServiceAccount::fromJsonFile($this->firebase_credentials_path);
            
            $this->firebase = (new Factory)
                ->withServiceAccount($serviceAccount)
                ->withDatabaseUri($this->firebase_database_url)
                ->create();
            
            $this->database = $this->firebase->getDatabase();
            
            error_log("Firebase initialized successfully");
        } catch (Exception $e) {
            error_log("Failed to initialize Firebase: " . $e->getMessage());
            throw new Exception("Firebase initialization failed");
        }
    }
    
    /**
     * Initialize MySQL connection
     */
    private function initializeMySQL() {
        try {
            $dsn = "mysql:host={$this->mysql_host};dbname={$this->mysql_db};charset=utf8mb4";
            $options = [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES => false,
            ];
            
            $this->mysql_pdo = new PDO($dsn, $this->mysql_user, $this->mysql_pass, $options);
            
            error_log("MySQL initialized successfully");
        } catch (PDOException $e) {
            error_log("Failed to initialize MySQL: " . $e->getMessage());
            throw new Exception("MySQL initialization failed");
        }
    }
    
    /**
     * Verify user status in Firebase before allowing operations
     * 
     * @param string $firebase_uid User's Firebase UID
     * @return array Status result with user data
     */
    public function verifyUserStatus($firebase_uid) {
        try {
            // Fetch user data from Firebase Realtime Database
            $reference = $this->database->getReference('users/' . $firebase_uid);
            $snapshot = $reference->getSnapshot();
            
            if (!$snapshot->exists()) {
                return [
                    'success' => false,
                    'error' => 'User not found in database',
                    'code' => 'USER_NOT_FOUND'
                ];
            }
            
            $userData = $snapshot->getValue();
            
            // Check if user has required fields
            if (!isset($userData['status']) || !isset($userData['email'])) {
                return [
                    'success' => false,
                    'error' => 'Invalid user data structure',
                    'code' => 'INVALID_USER_DATA'
                ];
            }
            
            // Check user status
            switch ($userData['status']) {
                case 'approved':
                    // User is approved - allow operation
                    return [
                        'success' => true,
                        'user' => $userData,
                        'message' => 'User is approved for operations'
                    ];
                    
                case 'pending':
                    return [
                        'success' => false,
                        'error' => 'User account is pending admin approval',
                        'code' => 'USER_PENDING'
                    ];
                    
                case 'rejected':
                    return [
                        'success' => false,
                        'error' => 'User account has been rejected',
                        'code' => 'USER_REJECTED'
                    ];
                    
                default:
                    return [
                        'success' => false,
                        'error' => 'Unknown user status: ' . $userData['status'],
                        'code' => 'UNKNOWN_STATUS'
                    ];
            }
            
        } catch (Exception $e) {
            error_log("Error verifying user status: " . $e->getMessage());
            return [
                'success' => false,
                'error' => 'Failed to verify user status: ' . $e->getMessage(),
                'code' => 'VERIFICATION_ERROR'
            ];
        }
    }
    
    /**
     * Insert message into MySQL database with Firebase user verification
     * 
     * @param string $firebase_uid Sender's Firebase UID
     * @param string $room_id Chat room ID
     * @param string $message_content Message content
     * @param string $message_type Message type (text, image, etc.)
     * @return array Operation result
     */
    public function insertMessageWithVerification($firebase_uid, $room_id, $message_content, $message_type = 'text') {
        // First, verify user status
        $verification_result = $this->verifyUserStatus($firebase_uid);
        
        if (!$verification_result['success']) {
            return $verification_result;
        }
        
        $user_data = $verification_result['user'];
        
        try {
            // Insert message into MySQL
            $sql = "INSERT INTO messages (
                        sender_firebase_uid, 
                        sender_email, 
                        sender_name, 
                        room_id, 
                        message_content, 
                        message_type, 
                        created_at,
                        updated_at
                    ) VALUES (
                        :sender_firebase_uid, 
                        :sender_email, 
                        :sender_name, 
                        :room_id, 
                        :message_content, 
                        :message_type, 
                        NOW(),
                        NOW()
                    )";
            
            $stmt = $this->mysql_pdo->prepare($sql);
            $result = $stmt->execute([
                ':sender_firebase_uid' => $firebase_uid,
                ':sender_email' => $user_data['email'],
                ':sender_name' => $user_data['displayName'] ?? 'Unknown User',
                ':room_id' => $room_id,
                ':message_content' => $message_content,
                ':message_type' => $message_type
            ]);
            
            if ($result) {
                $message_id = $this->mysql_pdo->lastInsertId();
                
                // Update user's last activity timestamp in Firebase
                $this->updateUserLastActivity($firebase_uid);
                
                return [
                    'success' => true,
                    'message_id' => $message_id,
                    'message' => 'Message sent successfully'
                ];
            } else {
                return [
                    'success' => false,
                    'error' => 'Failed to insert message into database',
                    'code' => 'INSERT_FAILED'
                ];
            }
            
        } catch (PDOException $e) {
            error_log("MySQL error: " . $e->getMessage());
            return [
                'success' => false,
                'error' => 'Database error: ' . $e->getMessage(),
                'code' => 'DATABASE_ERROR'
            ];
        }
    }
    
    /**
     * Update user's last activity timestamp in Firebase
     * 
     * @param string $firebase_uid User's Firebase UID
     */
    private function updateUserLastActivity($firebase_uid) {
        try {
            $reference = $this->database->getReference('users/' . $firebase_uid);
            $reference->update([
                'lastActivity' => time() * 1000, // Timestamp in milliseconds
                'status' => 'online'
            ]);
            
            error_log("Updated last activity for user: " . $firebase_uid);
        } catch (Exception $e) {
            error_log("Failed to update user activity: " . $e->getMessage());
            // Don't fail the main operation if activity update fails
        }
    }
    
    /**
     * Get user information for approved users only
     * 
     * @param string $firebase_uid User's Firebase UID
     * @return array User information or error
     */
    public function getApprovedUserInfo($firebase_uid) {
        $verification_result = $this->verifyUserStatus($firebase_uid);
        
        if (!$verification_result['success']) {
            return $verification_result;
        }
        
        return [
            'success' => true,
            'user' => $verification_result['user']
        ];
    }
    
    /**
     * Search for approved users only
     * 
     * @param string $search_query Search term
     * @param int $limit Maximum results
     * @return array Search results
     */
    public function searchApprovedUsers($search_query, $limit = 20) {
        try {
            $reference = $this->database->getReference('users');
            $snapshot = $reference->getSnapshot();
            
            if (!$snapshot->exists()) {
                return [
                    'success' => true,
                    'users' => [],
                    'message' => 'No users found'
                ];
            }
            
            $all_users = $snapshot->getValue();
            $approved_users = [];
            $search_lower = strtolower($search_query);
            
            foreach ($all_users as $uid => $user_data) {
                // Only include approved users
                if (isset($user_data['status']) && $user_data['status'] === 'approved') {
                    $email_match = isset($user_data['email']) && 
                                   strpos(strtolower($user_data['email']), $search_lower) !== false;
                    $name_match = isset($user_data['displayName']) && 
                                  strpos(strtolower($user_data['displayName']), $search_lower) !== false;
                    
                    if ($email_match || $name_match) {
                        // Remove sensitive information
                        unset($user_data['registrationTimestamp']);
                        unset($user_data['lastLoginTimestamp']);
                        
                        $approved_users[] = $user_data;
                        
                        // Apply limit
                        if (count($approved_users) >= $limit) {
                            break;
                        }
                    }
                }
            }
            
            return [
                'success' => true,
                'users' => $approved_users,
                'count' => count($approved_users)
            ];
            
        } catch (Exception $e) {
            error_log("Error searching users: " . $e->getMessage());
            return [
                'success' => false,
                'error' => 'Failed to search users: ' . $e->getMessage(),
                'code' => 'SEARCH_ERROR'
            ];
        }
    }
}

// Example usage and API endpoints
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    header('Content-Type: application/json');
    
    try {
        $firebase_verification = new FirebaseUserVerification();
        $input = json_decode(file_get_contents('php://input'), true);
        
        if (!$input) {
            throw new Exception('Invalid JSON input');
        }
        
        $action = $input['action'] ?? '';
        $firebase_uid = $input['firebase_uid'] ?? '';
        
        if (empty($firebase_uid)) {
            throw new Exception('Firebase UID is required');
        }
        
        switch ($action) {
            case 'verify_user':
                $result = $firebase_verification->verifyUserStatus($firebase_uid);
                echo json_encode($result);
                break;
                
            case 'send_message':
                $room_id = $input['room_id'] ?? '';
                $message_content = $input['message_content'] ?? '';
                $message_type = $input['message_type'] ?? 'text';
                
                if (empty($room_id) || empty($message_content)) {
                    throw new Exception('Room ID and message content are required');
                }
                
                $result = $firebase_verification->insertMessageWithVerification(
                    $firebase_uid, 
                    $room_id, 
                    $message_content, 
                    $message_type
                );
                echo json_encode($result);
                break;
                
            case 'get_user_info':
                $result = $firebase_verification->getApprovedUserInfo($firebase_uid);
                echo json_encode($result);
                break;
                
            case 'search_users':
                $search_query = $input['search_query'] ?? '';
                $limit = $input['limit'] ?? 20;
                
                if (empty($search_query)) {
                    throw new Exception('Search query is required');
                }
                
                $result = $firebase_verification->searchApprovedUsers($search_query, $limit);
                echo json_encode($result);
                break;
                
            default:
                throw new Exception('Invalid action: ' . $action);
        }
        
    } catch (Exception $e) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'error' => $e->getMessage(),
            'code' => 'REQUEST_ERROR'
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'error' => 'Method not allowed',
        'code' => 'METHOD_NOT_ALLOWED'
    ]);
}

/**
 * MySQL Table Structure for Messages:
 * 
 * CREATE TABLE messages (
 *     id INT PRIMARY KEY AUTO_INCREMENT,
 *     sender_firebase_uid VARCHAR(128) NOT NULL,
 *     sender_email VARCHAR(255) NOT NULL,
 *     sender_name VARCHAR(255) NOT NULL,
 *     room_id VARCHAR(255) NOT NULL,
 *     message_content TEXT NOT NULL,
 *     message_type ENUM('text', 'image', 'file', 'audio') DEFAULT 'text',
 *     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 *     
 *     INDEX idx_sender_uid (sender_firebase_uid),
 *     INDEX idx_room_id (room_id),
 *     INDEX idx_created_at (created_at)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
 */
?>