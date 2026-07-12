const BASE_URL = 'http://localhost:8080';

const getHeaders = () => {
    const token = localStorage.getItem('token');
    return {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    };
};

export const getActivities = async () => {
    const userId = localStorage.getItem('userId');
    if (!userId) {
        throw new Error('User not logged in');
    }
    const response = await fetch(`${BASE_URL}/api/activities/user/${userId}`, {
        method: 'GET',
        headers: getHeaders()
    });
    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }
    const data = await response.json();
    return { data };
};

export const getActivityDetail = async (id) => {
    // 1. Fetch activity details
    const activityResponse = await fetch(`${BASE_URL}/api/activities/${id}`, {
        method: 'GET',
        headers: getHeaders()
    });
    if (!activityResponse.ok) {
        throw new Error(`HTTP error! status: ${activityResponse.status}`);
    }
    const activityData = await activityResponse.json();

    // 2. Fetch AI recommendation for this activity
    let recommendationData = null;
    try {
        const recommendationResponse = await fetch(`${BASE_URL}/api/recommendation/activity/${id}`, {
            method: 'GET',
            headers: getHeaders()
        });
        if (recommendationResponse.ok) {
            recommendationData = await recommendationResponse.json();
        }
    } catch (error) {
        console.error('Error fetching recommendation:', error);
    }

    // Merge recommendation fields into the activity object as expected by ActivityDetail.jsx
    if (recommendationData) {
        activityData.recommendation = recommendationData.recommendation;
        activityData.improvements = recommendationData.improvements;
        activityData.suggestions = recommendationData.suggestions;
        activityData.safety = recommendationData.safetyMeasures;
    }

    return { data: activityData };
};

export const addActivity = async (activity) => {
    const userId = localStorage.getItem('userId');
    if (!userId) {
        throw new Error('User not logged in');
    }
    const payload = {
        ...activity,
        userId: userId,
        startTime: new Date().toISOString()
    };
    const response = await fetch(`${BASE_URL}/api/activities`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(payload)
    });
    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }
    const data = await response.json();
    return { data };
};
